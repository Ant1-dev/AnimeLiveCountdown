package com.Toine.animeCountdownBackend.anilist;

import com.Toine.animeCountdownBackend.anilist.AniListModels.MediaListEntry;
import com.Toine.animeCountdownBackend.models.postgreEntities.AniListAccountEntity;
import com.Toine.animeCountdownBackend.repositories.AniListAccountRepository;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Pushes changes made here to the user's AniList list (two-way sync; the import covers the other way).
 *
 * Runs after the database change commits, on one background thread, so following a show or marking an
 * episode watched never waits on AniList. Failures are logged and dropped: the site's own data is the
 * source of truth, and the next change (or a re-import) catches up.
 */
@Service
public class AniListSyncService {
    private static final Logger logger = LoggerFactory.getLogger(AniListSyncService.class);

    private final AniListAccountRepository accountRepository;
    private final AniListApiClient api;
    private final AniListTokenCipher cipher;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "anilist-sync");
        t.setDaemon(true);
        return t;
    });

    public AniListSyncService(AniListAccountRepository accountRepository, AniListApiClient api, AniListTokenCipher cipher) {
        this.accountRepository = accountRepository;
        this.api = api;
        this.cipher = cipher;
    }

    /** The user followed a show here. */
    public void followed(long userId, long mediaId) {
        afterCommit(() -> sync(userId, mediaId, AniListSyncPlanner::onFollow));
    }

    /** The user set their progress here. */
    public void progressed(long userId, long mediaId, int watchedEpisode, Integer totalEpisodes) {
        afterCommit(() -> sync(userId, mediaId, existing -> AniListSyncPlanner.onProgress(existing, watchedEpisode, totalEpisodes)));
    }

    private interface Plan {
        Optional<AniListSyncPlanner.Save> decide(MediaListEntry existing);
    }

    private void sync(long userId, long mediaId, Plan plan) {
        Optional<AniListAccountEntity> account = accountRepository.findByUserId(userId);
        if (account.isEmpty() || !account.get().isSyncEnabled()) {
            return;
        }
        AniListAccountEntity a = account.get();
        if (a.getTokenExpiresAt() != null && a.getTokenExpiresAt().isBefore(Instant.now())) {
            return; // Expired: the AniList panel asks the user to reconnect
        }
        try {
            String token = cipher.decrypt(a.getEncryptedAccessToken());
            MediaListEntry existing = api.fetchListEntry(token, a.getAniListUserId(), mediaId);
            Optional<AniListSyncPlanner.Save> save = plan.decide(existing);
            if (save.isPresent()) {
                api.saveListEntry(token, mediaId, save.get().status(), save.get().progress());
                logger.info("AniList sync: user {} show {} -> {} {}", userId, mediaId, save.get().status(),
                        save.get().progress() == null ? "" : "ep " + save.get().progress());
            }
        } catch (RuntimeException e) {
            logger.warn("AniList sync failed for user {} show {}: {}", userId, mediaId, e.getMessage());
        }
    }

    private void afterCommit(Runnable task) {
        Runnable submit = () -> executor.submit(task);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    submit.run();
                }
            });
        } else {
            submit.run();
        }
    }

    @PreDestroy
    void shutdown() {
        executor.shutdown();
    }
}
