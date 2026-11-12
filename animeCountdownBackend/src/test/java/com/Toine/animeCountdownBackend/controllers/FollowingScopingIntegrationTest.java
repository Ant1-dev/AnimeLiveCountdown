package com.Toine.animeCountdownBackend.controllers;

import com.Toine.animeCountdownBackend.anilist.AniListApiClient;
import com.Toine.animeCountdownBackend.anilist.AniListModels;
import com.Toine.animeCountdownBackend.anilist.AniListTokenCipher;
import com.Toine.animeCountdownBackend.models.postgreEntities.AniListAccountEntity;
import com.Toine.animeCountdownBackend.models.postgreEntities.FavoriteMediaEntity;
import com.Toine.animeCountdownBackend.models.postgreEntities.MediaEntity;
import com.Toine.animeCountdownBackend.models.postgreEntities.UserEntity;
import com.Toine.animeCountdownBackend.repositories.AniListAccountRepository;
import com.Toine.animeCountdownBackend.repositories.FavoriteMediaRepository;
import com.Toine.animeCountdownBackend.repositories.MediaRepository;
import com.Toine.animeCountdownBackend.repositories.UserRepository;
import com.Toine.animeCountdownBackend.services.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end scoping checks through the real security chain, JWT filter and database.
 * Each test runs in a transaction that is rolled back, so nothing is left in the shared dev DB.
 * Media IDs are far above AniList's range so they can't clash with synced shows.
 */
@SpringBootTest(properties = {
        "app.sync.airing.initial-delay=PT10H",
        "app.sync.upcoming.initial-delay=PT10H",
        "app.sync.just-aired.initial-delay=PT10H",
        "app.sync.media-info.initial-delay=PT10H",
        "app.anilist.client-id=test-client",
        "app.anilist.client-secret=test-secret"
})
@AutoConfigureMockMvc
@Transactional
class FollowingScopingIntegrationTest {
    private static final long SHOW_A = 9_000_000_001L;
    private static final long SHOW_B = 9_000_000_002L;
    private static final long SHOW_IMPORTED = 9_000_000_003L;

    @Autowired private MockMvc mvc;
    @Autowired private JwtService jwtService;
    @Autowired private UserRepository users;
    @Autowired private MediaRepository media;
    @Autowired private FavoriteMediaRepository favorites;
    @Autowired private AniListAccountRepository accounts;
    @Autowired private AniListTokenCipher cipher;
    @MockitoBean private AniListApiClient aniListApi;

    private UserEntity alice;
    private UserEntity bob;

    @BeforeEach
    void setUp() {
        alice = user("alice");
        bob = user("bob");
        media.save(show(SHOW_A, 5));
        media.save(show(SHOW_B, 9));
        follow(alice, SHOW_A, 1);
        follow(bob, SHOW_B, 2);
    }

    @Test
    void listShowsOnlyYourOwnFollows() throws Exception {
        mvc.perform(get("/api/auth/following").header("Authorization", bearer(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].media.id").value(SHOW_A))
                .andExpect(jsonPath("$[0].watchedEpisode").value(1))
                .andExpect(jsonPath("$[0].latestAiredEpisode").value(4))
                .andExpect(jsonPath("$[0].behind").value(3));
    }

    @Test
    void cannotChangeProgressOnSomeoneElsesFollow() throws Exception {
        mvc.perform(put("/api/auth/following/" + SHOW_B + "/progress")
                        .header("Authorization", bearer(alice))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"watchedEpisode\": 8}"))
                .andExpect(status().isNotFound());

        assertEquals(2, favorites.findFollow(bob.getId(), SHOW_B).orElseThrow().getWatchedEpisode());
    }

    @Test
    void canChangeProgressOnYourOwnFollow() throws Exception {
        mvc.perform(put("/api/auth/following/" + SHOW_A + "/progress")
                        .header("Authorization", bearer(alice))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"watchedEpisode\": 4}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.behind").value(0));

        assertEquals(4, favorites.findFollow(alice.getId(), SHOW_A).orElseThrow().getWatchedEpisode());
    }

    @Test
    void cannotUnfollowForSomeoneElse() throws Exception {
        mvc.perform(delete("/api/auth/following/" + SHOW_B).header("Authorization", bearer(alice)))
                .andExpect(status().isNotFound());

        assertEquals(true, favorites.findFollow(bob.getId(), SHOW_B).isPresent());
    }

    @Test
    void endpointsRequireSignIn() throws Exception {
        mvc.perform(get("/api/auth/following")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/auth/following/" + SHOW_A + "/progress")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"watchedEpisode\": 1}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/anilist/import")).andExpect(status().isUnauthorized());
    }

    @Test
    void myScheduleOnlyHasYourShows() throws Exception {
        String day = Instant.now().plus(1, ChronoUnit.HOURS).atZone(java.time.ZoneOffset.UTC).getDayOfWeek().toString();

        mvc.perform(get("/api/auth/following/airing/" + day).param("tz", "UTC").header("Authorization", bearer(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].media.id").value(SHOW_A));
    }

    @Test
    void anilistImportOnlyTouchesTheImportingUser() throws Exception {
        AniListAccountEntity account = new AniListAccountEntity();
        account.setUser(alice);
        account.setAniListUserId(123L);
        account.setEncryptedAccessToken(cipher.encrypt("alice-token"));
        accounts.save(account);
        when(aniListApi.fetchCurrentAndPlanning(eq("alice-token"), anyLong())).thenReturn(List.of(
                entry(SHOW_B, "RELEASING", 6),          // Bob follows this show; Alice now does too
                entry(SHOW_IMPORTED, "RELEASING", 1))); // Not in the table yet: added

        mvc.perform(post("/api/auth/anilist/import").header("Authorization", bearer(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.followed").value(2))
                .andExpect(jsonPath("$.showsAdded").value(1));

        assertEquals(6, favorites.findFollow(alice.getId(), SHOW_B).orElseThrow().getWatchedEpisode());
        assertEquals(1, favorites.findFollow(alice.getId(), SHOW_IMPORTED).orElseThrow().getWatchedEpisode());
        assertEquals(2, favorites.findFollow(bob.getId(), SHOW_B).orElseThrow().getWatchedEpisode());
        assertEquals(1, favorites.findFollowsWithMedia(bob.getId()).size());

        // Bob has no AniList account; he can't import into (or from) Alice's
        mvc.perform(post("/api/auth/anilist/import").header("Authorization", bearer(bob)))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/auth/anilist").header("Authorization", bearer(bob)))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.connected").value(false));
    }

    private String bearer(UserEntity user) {
        return "Bearer " + jwtService.generateToken(new java.util.HashMap<>(Map.of("id", user.getId())), user);
    }

    private UserEntity user(String name) {
        UserEntity u = new UserEntity();
        u.setEmail(name + "-" + UUID.randomUUID() + "@scoping.test");
        u.setName(name);
        return users.save(u);
    }

    private static MediaEntity show(long id, int nextEpisode) {
        MediaEntity m = new MediaEntity();
        m.setId(id);
        m.setTitle_Romaji("Scoping test " + id);
        m.setStatus("RELEASING");
        m.setNext_Airing_Episode(nextEpisode);
        m.setNext_Airing_At(Instant.now().plus(1, ChronoUnit.HOURS));
        return m;
    }

    private void follow(UserEntity user, long mediaId, int watched) {
        FavoriteMediaEntity f = new FavoriteMediaEntity();
        f.setUser(user);
        f.setMedia(media.getReferenceById(mediaId));
        f.setWatchedEpisode(watched);
        favorites.save(f);
    }

    private static AniListModels.ListEntry entry(long id, String status, int progress) {
        return new AniListModels.ListEntry("CURRENT", progress, new AniListModels.ListMedia(id, false, status, 12,
                1L, 2026, "FALL", null, new AniListModels.Title(null, "Imported " + id),
                new AniListModels.NextAiring(Instant.now().plus(2, ChronoUnit.DAYS).getEpochSecond(), 2), null));
    }
}
