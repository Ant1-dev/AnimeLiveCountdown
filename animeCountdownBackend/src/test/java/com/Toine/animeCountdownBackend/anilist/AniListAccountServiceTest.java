package com.Toine.animeCountdownBackend.anilist;

import com.Toine.animeCountdownBackend.anilist.AniListAccountService.AniListUnavailableException;
import com.Toine.animeCountdownBackend.models.postgreEntities.AniListAccountEntity;
import com.Toine.animeCountdownBackend.models.postgreEntities.FavoriteMediaEntity;
import com.Toine.animeCountdownBackend.models.postgreEntities.MediaEntity;
import com.Toine.animeCountdownBackend.models.postgreEntities.UserEntity;
import com.Toine.animeCountdownBackend.repositories.AniListAccountRepository;
import com.Toine.animeCountdownBackend.repositories.FavoriteMediaRepository;
import com.Toine.animeCountdownBackend.repositories.MediaRepository;
import com.Toine.animeCountdownBackend.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AniListAccountServiceTest {
    private static final String SECRET = Base64.getEncoder().encodeToString(new byte[32]);
    private static final Instant NOW = Instant.parse("2026-11-11T12:00:00Z");
    private static final long USER_ID = 7L;

    private AniListApiClient api;
    private AniListAccountRepository accounts;
    private FavoriteMediaRepository favorites;
    private MediaRepository media;
    private UserRepository users;
    private AniListTokenCipher cipher;
    private AniListOAuthState oauthState;
    private AniListAccountService service;
    private UserEntity user;
    private AniListAccountEntity account;

    @BeforeEach
    void setUp() {
        api = mock(AniListApiClient.class);
        accounts = mock(AniListAccountRepository.class);
        favorites = mock(FavoriteMediaRepository.class);
        media = mock(MediaRepository.class);
        users = mock(UserRepository.class);
        cipher = new AniListTokenCipher("jwt-secret");
        oauthState = new AniListOAuthState(SECRET, Clock.fixed(NOW, ZoneOffset.UTC));
        service = new AniListAccountService(new AniListOAuthSettings("client", "secret", "https://api.test/cb"),
                oauthState, cipher, api, accounts, favorites, media, users,
                new TransactionTemplate(mock(PlatformTransactionManager.class)), null, Clock.fixed(NOW, ZoneOffset.UTC));

        user = new UserEntity();
        user.setId(USER_ID);
        account = new AniListAccountEntity();
        ReflectionTestUtils.setField(account, "id", 3L);
        account.setAniListUserId(555L);
        account.setEncryptedAccessToken(cipher.encrypt("user-token"));
        when(accounts.findByUserId(USER_ID)).thenReturn(Optional.of(account));
        when(accounts.findById(3L)).thenReturn(Optional.of(account));
        when(users.getReferenceById(USER_ID)).thenReturn(user);
        when(media.getReferenceById(anyLong())).thenAnswer(inv -> mediaWithId(inv.getArgument(0)));
    }

    @Test
    void importFetchesWithTheUsersOwnTokenAndAppliesThePlan() {
        when(api.fetchCurrentAndPlanning("user-token", 555L)).thenReturn(AniListFixtures.listEntries());
        when(media.findAllById(any())).thenReturn(List.of(mediaWithId(171018L), mediaWithId(160000L), mediaWithId(150000L)));
        FavoriteMediaEntity followedA = follow(171018L, 3);
        FavoriteMediaEntity followedB = follow(160000L, 4);
        when(favorites.findFollows(eq(USER_ID), anyCollection())).thenReturn(List.of(followedA, followedB));

        AniListAccountService.ImportResult result = service.importList(user);

        assertEquals(3, result.followed());
        assertEquals(1, result.progressUpdated());
        assertEquals(1, result.unchanged());
        assertEquals(2, result.showsAdded());
        assertEquals(1, result.skippedNotAiring());
        assertEquals(1, result.skippedAdult());
        assertEquals(NOW, account.getLastSyncedAt());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<FavoriteMediaEntity>> saved = ArgumentCaptor.forClass(List.class);
        verify(favorites).saveAll(saved.capture());
        Map<Long, Integer> progressByMedia = saved.getValue().stream()
                .collect(Collectors.toMap(f -> f.getMedia().getId(), FavoriteMediaEntity::getWatchedEpisode));
        assertEquals(Map.of(182255L, 3, 190000L, 0, 150000L, 0, 171018L, 5), progressByMedia);
        // Every follow created belongs to the importing user
        assertTrue(saved.getValue().stream().allMatch(f -> f.getUser() == user));
        verify(media).saveAll(argThat(list -> ((List<MediaEntity>) list).size() == 2));
    }

    @Test
    void importRequiresAConnectedAccount() {
        when(accounts.findByUserId(USER_ID)).thenReturn(Optional.empty());

        AniListUnavailableException e = assertThrows(AniListUnavailableException.class, () -> service.importList(user));

        assertEquals(AniListUnavailableException.Reason.NOT_CONNECTED, e.reason());
        verifyNoInteractions(api);
    }

    @Test
    void secondImportWithinCooldownIsRefused() {
        when(api.fetchCurrentAndPlanning(any(), anyLong())).thenReturn(List.of());
        when(media.findAllById(any())).thenReturn(List.of());

        service.importList(user);
        AniListUnavailableException e = assertThrows(AniListUnavailableException.class, () -> service.importList(user));

        assertEquals(AniListUnavailableException.Reason.COOLDOWN, e.reason());
        verify(api, times(1)).fetchCurrentAndPlanning(any(), anyLong());
    }

    @Test
    void featureIsOffWithoutCredentials() {
        AniListAccountService off = new AniListAccountService(new AniListOAuthSettings("", "", ""),
                oauthState, cipher, api, accounts, favorites, media, users,
                new TransactionTemplate(mock(PlatformTransactionManager.class)), null, Clock.fixed(NOW, ZoneOffset.UTC));

        assertFalse(off.status(user).enabled());
        assertEquals(AniListUnavailableException.Reason.NOT_CONFIGURED,
                assertThrows(AniListUnavailableException.class, () -> off.startConnect(user, "x")).reason());
        assertEquals(AniListUnavailableException.Reason.NOT_CONFIGURED,
                assertThrows(AniListUnavailableException.class, () -> off.importList(user)).reason());
    }

    @Test
    void connectLinksTheAccountToTheUserWhoStartedTheFlow() {
        AniListAccountService.ConnectStart start = service.startConnect(user, "https://api.test/cb");
        assertTrue(start.authorizeUrl().startsWith(AniListOAuthSettings.AUTHORIZE_URL + "?client_id=client"));
        assertTrue(start.authorizeUrl().contains("redirect_uri=https://api.test/cb"));
        assertTrue(start.authorizeUrl().contains("response_type=code"));
        String state = start.authorizeUrl().replaceAll(".*state=", "");

        when(users.findById(USER_ID)).thenReturn(Optional.of(user));
        when(accounts.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(api.exchangeCode("the-code", "https://api.test/cb"))
                .thenReturn(new AniListModels.TokenResponse("fresh-token", "Bearer", 31536000L));
        when(api.fetchViewer("fresh-token")).thenReturn(new AniListModels.Viewer(555L, "aniUser"));

        assertTrue(service.completeConnect("the-code", state, start.nonce(), "https://api.test/cb"));

        ArgumentCaptor<AniListAccountEntity> saved = ArgumentCaptor.forClass(AniListAccountEntity.class);
        verify(accounts).save(saved.capture());
        assertSame(user, saved.getValue().getUser());
        assertEquals("aniUser", saved.getValue().getAniListUserName());
        assertEquals("fresh-token", cipher.decrypt(saved.getValue().getEncryptedAccessToken()));
        assertEquals(NOW.plusSeconds(31536000L), saved.getValue().getTokenExpiresAt());
    }

    @Test
    void connectWithForeignStateNeverCallsAniList() {
        AniListAccountService.ConnectStart start = service.startConnect(user, "https://api.test/cb");
        String state = start.authorizeUrl().replaceAll(".*state=", "");

        assertFalse(service.completeConnect("code", state, "some-other-browser-nonce", "https://api.test/cb"));
        assertFalse(service.completeConnect("code", "garbage", start.nonce(), "https://api.test/cb"));
        verifyNoInteractions(api);
        verify(accounts, never()).save(any());
    }

    private static MediaEntity mediaWithId(long id) {
        MediaEntity m = new MediaEntity();
        m.setId(id);
        return m;
    }

    private FavoriteMediaEntity follow(long mediaId, int watched) {
        FavoriteMediaEntity f = new FavoriteMediaEntity();
        f.setUser(user);
        f.setMedia(mediaWithId(mediaId));
        f.setWatchedEpisode(watched);
        return f;
    }
}
