package com.Toine.animeCountdownBackend.services.discovery;

import com.Toine.animeCountdownBackend.models.discovery.LiveEpisode;
import com.Toine.animeCountdownBackend.services.discovery.LiveService.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class LiveServiceTest {

    private static Schedule schedule(long id, long airingAt, Boolean adult, Integer duration) {
        return new Schedule(4, airingAt, new ScheduleMedia(id, adult, duration, new Title("Show " + id, null), new Cover("c")));
    }

    @Test
    void keepsTrackedNonAdultShowsSortedByTime() {
        List<LiveEpisode> episodes = LiveService.toEpisodes(List.of(
                schedule(1, 2000, false, 24),
                schedule(2, 1000, false, null),
                schedule(3, 1500, true, 24),
                schedule(4, 1200, false, 24)), Set.of(1L, 2L, 3L));

        assertEquals(List.of(2L, 1L), episodes.stream().map(LiveEpisode::mediaId).toList());
        assertEquals(24, episodes.get(0).durationMinutes()); // missing duration defaults to 24 min
        assertEquals("Show 2", episodes.get(0).engtitle());
    }

    @Test
    void handlesMissingSchedules() {
        assertEquals(List.of(), LiveService.toEpisodes(null, Set.of()));
    }
}
