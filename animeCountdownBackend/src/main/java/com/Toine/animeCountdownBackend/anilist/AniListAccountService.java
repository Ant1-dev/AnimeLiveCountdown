package com.Toine.animeCountdownBackend.anilist;

import com.Toine.animeCountdownBackend.anilist.AniListModels.ListEntry;
import com.Toine.animeCountdownBackend.anilist.AniListModels.TokenResponse;
import com.Toine.animeCountdownBackend.anilist.AniListModels.Viewer;
import com.Toine.animeCountdownBackend.models.postgreEntities.AniListAccountEntity;
import com.Toine.animeCountdownBackend.models.postgreEntities.FavoriteMediaEntity;
import com.Toine.animeCountdownBackend.models.postgreEntities.MediaEntity;
import com.Toine.animeCountdownBackend.models.postgreEntities.UserEntity;
import com.Toine.animeCountdownBackend.repositories.AniListAccountRepository;
import com.Toine.animeCountdownBackend.repositories.FavoriteMediaRepository;
import com.Toine.animeCountdownBackend.repositories.MediaRepository;
import com.Toine.animeCountdownBackend.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/** Links AniList accounts to users and imports their CURRENT / PLANNING lists as follows. */
@Service
public class AniListAccountService {
    private static final Logger logger = LoggerFactory.getLogger(AniListAccountService.class);
    static final Duration IMPORT_COOLDOWN = Duration.ofSeconds(30);

    public record Status(boolean enabled, boolean connected, String userName, Instant lastSyncedAt) {
    }

    public record ConnectStart(String authorizeUrl, String nonce) {
    }

    public record ImportResult(
            int entries,
            int followed,
            int progressUpdated,
            int unchanged,
            int showsAdded,
            int skippedNotAiring,
            int skippedAdult,
            Instant syncedAt) {
    }

    /** Thrown when the user asks for something AniList-related that can't happen right now. */
    public static class AniListUnavailableException extends RuntimeException {
        public enum Reason { NOT_CONFIGURED, NOT_CONNECTED, TOKEN_INVALID, COOLDOWN, ANILIST_ERROR }

        private final Reason reason;

        public AniListUnavailableException(Reason reason, String message) {
            super(message);
            this.reason = reason;
        }

        public Reason reason() {
            return reason;
        }
    }

    private final AniListOAuthSettings settings;
    private final AniListOAuthState oauthState;
    private final AniListTokenCipher cipher;
    private final AniListApiClient api;
    private final AniListAccountRepository accountRepository;
    private final FavoriteMediaRepository favoriteRepository;
    private final MediaRepository mediaRepository;
    private final UserRepository userRepository;
    private final TransactionTemplate tx;
    private final CacheManager cacheManager;
    private final Clock clock;
    private final Map<Long, Instant> lastImportStart = new ConcurrentHashMap<>();

    @org.springframework.beans.factory.annotation.Autowired
    public AniListAccountService(AniListOAuthSettings settings, AniListOAuthState oauthState, AniListTokenCipher cipher,
                                 AniListApiClient api, AniListAccountRepository accountRepository,
                                 FavoriteMediaRepository favoriteRepository, MediaRepository mediaRepository,
                                 UserRepository userRepository, PlatformTransactionManager transactionManager,
                                 CacheManager cacheManager) {
        this(settings, oauthState, cipher, api, accountRepository, favoriteRepository, mediaRepository,
                userRepository, new TransactionTemplate(transactionManager), cacheManager, Clock.systemUTC());
    }

    AniListAccountService(AniListOAuthSettings settings, AniListOAuthState oauthState, AniListTokenCipher cipher,
                          AniListApiClient api, AniListAccountRepository accountRepository,
                          FavoriteMediaRepository favoriteRepository, MediaRepository mediaRepository,
                          UserRepository userRepository, TransactionTemplate tx, CacheManager cacheManager, Clock clock) {
        this.settings = settings;
        this.oauthState = oauthState;
        this.cipher = cipher;
        this.api = api;
        this.accountRepository = accountRepository;
        this.favoriteRepository = favoriteRepository;
        this.mediaRepository = mediaRepository;
        this.userRepository = userRepository;
        this.tx = tx;
        this.cacheManager = cacheManager;
        this.clock = clock;
    }

    public Status status(UserEntity user) {
        if (!settings.isEnabled()) {
            return new Status(false, false, null, null);
        }
        return accountRepository.findByUserId(user.getId())
                .map(a -> new Status(true, true, a.getAniListUserName(), a.getLastSyncedAt()))
                .orElse(new Status(true, false, null, null));
    }

    /** Builds the AniList authorize URL for this user. The returned nonce must be set as the flow cookie. */
    public ConnectStart startConnect(UserEntity user, String redirectUri) {
        requireConfigured();
        AniListOAuthState.Issued issued = oauthState.issue(user.getId());
        String url = UriComponentsBuilder.fromUriString(settings.authorizeUrl())
                .queryParam("client_id", settings.clientId())
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("state", issued.state())
                .encode()
                .build()
                .toUriString();
        return new ConnectStart(url, issued.nonce());
    }

    /**
     * Finishes the OAuth flow: checks the state against the cookie nonce, exchanges the code and
     * stores the (encrypted) token on the user who started the flow.
     *
     * @return false if the state or cookie didn't check out
     */
    public boolean completeConnect(String code, String state, String cookieNonce, String redirectUri) {
        requireConfigured();
        Optional<Long> userId = oauthState.verify(state, cookieNonce);
        if (userId.isEmpty() || code == null || code.isBlank()) {
            return false;
        }
        Optional<UserEntity> user = userRepository.findById(userId.get());
        if (user.isEmpty()) {
            return false;
        }

        TokenResponse token = api.exchangeCode(code, redirectUri);
        Viewer viewer = api.fetchViewer(token.accessToken());
        Instant now = clock.instant();

        tx.executeWithoutResult(status -> {
            AniListAccountEntity account = accountRepository.findByUserId(user.get().getId())
                    .orElseGet(AniListAccountEntity::new);
            account.setUser(userRepository.getReferenceById(user.get().getId()));
            account.setAniListUserId(viewer.id());
            account.setAniListUserName(viewer.name());
            account.setEncryptedAccessToken(cipher.encrypt(token.accessToken()));
            account.setTokenExpiresAt(token.expiresIn() == null ? null : now.plusSeconds(token.expiresIn()));
            account.setConnectedAt(now);
            accountRepository.save(account);
        });
        logger.info("Linked AniList account {} to user {}", viewer.id(), user.get().getId());
        return true;
    }

    public void disconnect(UserEntity user) {
        accountRepository.deleteByUserId(user.getId());
    }

    /** Imports the user's CURRENT and PLANNING entries. Safe to run again (re-sync). */
    public ImportResult importList(UserEntity user) {
        requireConfigured();
        AniListAccountEntity account = accountRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AniListUnavailableException(
                        AniListUnavailableException.Reason.NOT_CONNECTED, "AniList isn't connected"));

        Instant now = clock.instant();
        Instant previous = lastImportStart.get(user.getId());
        if (previous != null && previous.plus(IMPORT_COOLDOWN).isAfter(now)) {
            throw new AniListUnavailableException(AniListUnavailableException.Reason.COOLDOWN,
                    "An import just ran. Try again in a few seconds.");
        }
        lastImportStart.put(user.getId(), now);

        String token;
        try {
            token = cipher.decrypt(account.getEncryptedAccessToken());
        } catch (IllegalArgumentException e) {
            throw new AniListUnavailableException(AniListUnavailableException.Reason.TOKEN_INVALID,
                    "Reconnect AniList to sync again");
        }
        if (account.getTokenExpiresAt() != null && account.getTokenExpiresAt().isBefore(now)) {
            throw new AniListUnavailableException(AniListUnavailableException.Reason.TOKEN_INVALID,
                    "Your AniList connection expired. Reconnect AniList to sync again");
        }

        List<ListEntry> entries;
        try {
            entries = api.fetchCurrentAndPlanning(token, account.getAniListUserId());
        } catch (RuntimeException e) {
            if (isUnauthorized(e)) {
                throw new AniListUnavailableException(AniListUnavailableException.Reason.TOKEN_INVALID,
                        "AniList rejected the connection. Reconnect AniList to sync again");
            }
            logger.warn("AniList list fetch failed for user {}: {}", user.getId(), e.getMessage());
            throw new AniListUnavailableException(AniListUnavailableException.Reason.ANILIST_ERROR,
                    "Couldn't reach AniList. Try again in a minute");
        }

        ImportResult result = tx.execute(status -> apply(user.getId(), account.getId(), entries, now));
        evictScheduleCaches(result);
        logger.info("AniList import for user {}: {}", user.getId(), result);
        return result;
    }

    private ImportResult apply(long userId, long accountId, List<ListEntry> entries, Instant now) {
        Set<Long> ids = entries.stream()
                .filter(e -> e != null && e.media() != null && e.media().id() != null)
                .map(e -> e.media().id())
                .collect(Collectors.toSet());

        Set<Long> mediaInDb = mediaRepository.findAllById(ids).stream()
                .map(MediaEntity::getId)
                .collect(Collectors.toSet());
        Map<Long, FavoriteMediaEntity> follows = ids.isEmpty() ? Map.of()
                : favoriteRepository.findFollows(userId, ids).stream()
                .collect(Collectors.toMap(f -> f.getMedia().getId(), f -> f));
        Map<Long, Integer> followedProgress = follows.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getWatchedEpisode()));

        AniListImportPlanner.Plan plan = AniListImportPlanner.plan(entries, mediaInDb, followedProgress);

        if (!plan.mediaToInsert().isEmpty()) {
            mediaRepository.saveAll(plan.mediaToInsert());
        }

        UserEntity userRef = userRepository.getReferenceById(userId);
        List<FavoriteMediaEntity> toSave = new ArrayList<>();
        plan.newFollows().forEach((mediaId, progress) -> {
            FavoriteMediaEntity follow = new FavoriteMediaEntity();
            follow.setUser(userRef);
            follow.setMedia(mediaRepository.getReferenceById(mediaId));
            follow.setAddedDate(now);
            follow.setWatchedEpisode(progress);
            follow.setProgressUpdatedAt(now);
            toSave.add(follow);
        });
        plan.progressRaises().forEach((mediaId, progress) -> {
            FavoriteMediaEntity follow = follows.get(mediaId);
            follow.setWatchedEpisode(progress);
            follow.setProgressUpdatedAt(now);
            toSave.add(follow);
        });
        if (!toSave.isEmpty()) {
            favoriteRepository.saveAll(toSave);
        }

        accountRepository.findById(accountId).ifPresent(account -> {
            account.setLastSyncedAt(now);
            accountRepository.save(account);
        });

        return new ImportResult(plan.entriesConsidered(), plan.newFollows().size(), plan.progressRaises().size(),
                plan.unchanged(), plan.mediaToInsert().size(), plan.skippedNotAiring(), plan.skippedAdult(), now);
    }

    private void evictScheduleCaches(ImportResult result) {
        if (result == null || result.showsAdded() == 0 || cacheManager == null) {
            return;
        }
        for (String name : List.of("weekdayAnime", "upcomingAnime", "trendingAnime")) {
            var cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        }
    }

    private void requireConfigured() {
        if (!settings.isEnabled()) {
            throw new AniListUnavailableException(AniListUnavailableException.Reason.NOT_CONFIGURED,
                    "AniList connect isn't set up on this server");
        }
    }

    private static boolean isUnauthorized(Throwable error) {
        for (Throwable t = error; t != null && t.getCause() != t; t = t.getCause()) {
            if (t instanceof org.springframework.web.reactive.function.client.WebClientResponseException w
                    && w.getStatusCode().value() == 401) {
                return true;
            }
            if (t.getMessage() != null && (t.getMessage().contains("401") || t.getMessage().contains("Invalid token"))) {
                return true;
            }
        }
        return false;
    }
}
