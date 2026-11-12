package com.Toine.animeCountdownBackend.services;

import com.Toine.animeCountdownBackend.dto.FollowView;
import com.Toine.animeCountdownBackend.models.postgreEntities.FavoriteMediaEntity;
import com.Toine.animeCountdownBackend.models.postgreEntities.MediaEntity;
import com.Toine.animeCountdownBackend.models.postgreEntities.MediaInfoEntity;
import com.Toine.animeCountdownBackend.repositories.FavoriteMediaRepository;
import com.Toine.animeCountdownBackend.repositories.MediaInfoRepository;
import com.Toine.animeCountdownBackend.repositories.MediaRepository;
import com.Toine.animeCountdownBackend.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FollowingServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-11T12:00:00Z"); // a Wednesday
    private static final long ME = 1L;

    private FavoriteMediaRepository favorites;
    private MediaInfoRepository mediaInfo;
    private FollowingService service;

    @BeforeEach
    void setUp() {
        favorites = mock(FavoriteMediaRepository.class);
        mediaInfo = mock(MediaInfoRepository.class);
        service = new FollowingService(favorites, mock(MediaRepository.class), mediaInfo,
                mock(UserRepository.class), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void listReportsProgressAndHowFarBehind() {
        FavoriteMediaEntity airing = follow(10L, "RELEASING", 7, NOW.plusSeconds(3600), 2);
        FavoriteMediaEntity finished = follow(11L, "FINISHED", null, null, 10);
        when(favorites.findFollowsWithMedia(ME)).thenReturn(List.of(airing, finished));
        MediaInfoEntity info = new MediaInfoEntity();
        info.setId(11L);
        info.setTotalepisodes(12);
        when(mediaInfo.findAllById(List.of(11L))).thenReturn(List.of(info));

        List<FollowView> views = service.list(ME);

        assertEquals(6, views.get(0).latestAiredEpisode());
        assertEquals(4, views.get(0).behind());
        assertEquals(12, views.get(1).latestAiredEpisode());
        assertEquals(2, views.get(1).behind());
    }

    @Test
    void progressCanOnlyBeSetOnYourOwnFollow() {
        // The show exists and someone else follows it, but this user doesn't: the lookup is by user
        when(favorites.findFollow(ME, 10L)).thenReturn(Optional.empty());

        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> service.setProgress(ME, 10L, 3));

        assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        verify(favorites, never()).save(any());
    }

    @Test
    void progressIsSavedWithinAiredEpisodes() {
        FavoriteMediaEntity f = follow(10L, "RELEASING", 7, NOW.plusSeconds(3600), 2);
        when(favorites.findFollow(ME, 10L)).thenReturn(Optional.of(f));

        FollowView view = service.setProgress(ME, 10L, 6);

        assertEquals(6, f.getWatchedEpisode());
        assertEquals(NOW, f.getProgressUpdatedAt());
        assertEquals(0, view.behind());
        verify(favorites).save(f);
    }

    @Test
    void progressBeyondTheLatestAiredEpisodeOrNegativeIsRejected() {
        FavoriteMediaEntity f = follow(10L, "RELEASING", 7, NOW.plusSeconds(3600), 2);
        when(favorites.findFollow(ME, 10L)).thenReturn(Optional.of(f));

        assertEquals(HttpStatus.BAD_REQUEST,
                assertThrows(ResponseStatusException.class, () -> service.setProgress(ME, 10L, 7)).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST,
                assertThrows(ResponseStatusException.class, () -> service.setProgress(ME, 10L, -1)).getStatusCode());
        assertEquals(2, f.getWatchedEpisode());
    }

    @Test
    void unfollowingAShowYouDontFollowIs404() {
        when(favorites.removeFavoriteMedia(ME, 10L)).thenReturn(0);

        assertThrows(ResponseStatusException.class, () -> service.unfollow(ME, 10L));
    }

    @Test
    void mySchedulePicksFollowedShowsAiringThatDayInTheViewersTimezone() {
        // Thursday 02:00 UTC = Wednesday 21:00 in New York
        FavoriteMediaEntity lateWednesdayNy = follow(20L, "RELEASING", 3, Instant.parse("2026-11-12T02:00:00Z"), 0);
        FavoriteMediaEntity thursday = follow(21L, "RELEASING", 5, Instant.parse("2026-11-12T18:00:00Z"), 0);
        FavoriteMediaEntity finished = follow(22L, "FINISHED", null, null, 0);
        when(favorites.findFollowsWithMedia(ME)).thenReturn(List.of(thursday, lateWednesdayNy, finished));

        List<Long> wednesdayNy = ids(service.airingOn(ME, DayOfWeek.WEDNESDAY, ZoneId.of("America/New_York")));
        List<Long> thursdayUtc = ids(service.airingOn(ME, DayOfWeek.THURSDAY, ZoneOffset.UTC));

        assertEquals(List.of(20L), wednesdayNy);
        assertEquals(List.of(20L, 21L), thursdayUtc);
    }

    private static List<Long> ids(List<FollowView> views) {
        return views.stream().map(v -> v.media().getId()).toList();
    }

    private static FavoriteMediaEntity follow(long mediaId, String status, Integer nextEp, Instant nextAt, int watched) {
        MediaEntity m = new MediaEntity();
        m.setId(mediaId);
        m.setStatus(status);
        m.setNext_Airing_Episode(nextEp);
        m.setNext_Airing_At(nextAt);
        FavoriteMediaEntity f = new FavoriteMediaEntity();
        f.setMedia(m);
        f.setWatchedEpisode(watched);
        return f;
    }
}
