package com.Toine.animeCountdownBackend.services.notifications;

import com.Toine.animeCountdownBackend.models.postgreEntities.MediaEntity;
import com.Toine.animeCountdownBackend.models.postgreEntities.ReleaseTimeEntity;
import com.Toine.animeCountdownBackend.models.postgreEntities.ScheduleNote;
import com.Toine.animeCountdownBackend.repositories.DelayAlertRepository;
import com.Toine.animeCountdownBackend.repositories.MediaRepository;
import com.Toine.animeCountdownBackend.repositories.ReleaseTimeRepository;
import com.Toine.animeCountdownBackend.repositories.ReminderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DelayAlertServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-22T12:00:00Z");
    private static final Instant DETECTED = NOW.minus(Duration.ofMinutes(3));
    private static final Instant NEW_AT = Instant.parse("2026-12-06T15:00:00Z");

    private MediaRepository media;
    private ReleaseTimeRepository releases;
    private ReminderRepository reminders;
    private DelayAlertRepository delayAlerts;
    private WebPushService push;
    private DiscordWebhookService discord;
    private ReminderDispatchService dispatch;
    private DelayAlertService service;

    @BeforeEach
    void setUp() {
        media = mock(MediaRepository.class);
        releases = mock(ReleaseTimeRepository.class);
        reminders = mock(ReminderRepository.class);
        delayAlerts = mock(DelayAlertRepository.class);
        push = mock(WebPushService.class);
        discord = mock(DiscordWebhookService.class);
        dispatch = mock(ReminderDispatchService.class);
        when(push.isEnabled()).thenReturn(true);
        when(discord.isEnabled()).thenReturn(true);
        service = new DelayAlertService(media, releases, reminders, delayAlerts, push, discord, dispatch,
                "https://animelivecountdown.com/");
    }

    private static MediaEntity show(ScheduleNote.Type type, Instant expiresAt) {
        MediaEntity m = new MediaEntity();
        m.setId(10L);
        m.setTitle_English("Frieren");
        m.setCover_Image_Url("https://img/1.png");
        m.setNext_Airing_Episode(7);
        m.setNext_Airing_At(expiresAt);
        m.setScheduleNoteType(type);
        m.setScheduleNoteDetectedAt(DETECTED);
        m.setScheduleNoteExpiresAt(expiresAt);
        m.setScheduleNoteAlertPending(true);
        return m;
    }

    @Test
    void aniListDelayAlertsFollowersOnEveryChosenChannel() {
        when(media.findPendingDelayAlerts()).thenReturn(List.of(show(ScheduleNote.Type.DELAYED, NEW_AT)));
        when(delayAlerts.upsertForFollowers(anyLong(), anyInt(), any(), anyBoolean(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(1L, 2L, 3L));
        when(reminders.findChannels(10L, List.of(1L, 2L, 3L))).thenReturn(List.of(
                new ReminderChannels(1L, true, true), new ReminderChannels(2L, false, false)));

        assertEquals(3, service.alertPending(NOW));

        verify(delayAlerts).upsertForFollowers(eq(10L), eq(7), eq("DELAY"), eq(false), eq(NEW_AT), eq(DETECTED), eq(NOW),
                eq("Frieren"), eq("Episode 7 delayed to Sun, Dec 6, 15:00 UTC"), eq("https://img/1.png"));
        ArgumentCaptor<PushMessage> message = ArgumentCaptor.forClass(PushMessage.class);
        verify(push, times(1)).sendToUser(eq(1L), message.capture(), any());
        assertEquals("Episode 7 delayed to Sun, Dec 6, 15:00 UTC", message.getValue().body());
        assertEquals("m10e7delay", message.getValue().tag());
        verify(dispatch).sendDiscord(eq(1L), eq("Frieren"), eq("Episode 7 delayed to <t:" + NEW_AT.getEpochSecond() + ":F>"),
                eq("https://animelivecountdown.com/anime/10/frieren"), any(), any());
        verify(push, never()).sendToUser(eq(2L), any(), any());
        verify(media).clearDelayAlertPending(10L, NEW_AT);
    }

    @Test
    void breakSaysWhenTheShowIsBack() {
        DelayAlertService.Delay delay = service.fromScheduleNote(show(ScheduleNote.Type.ON_BREAK, NEW_AT), NOW);

        assertEquals("On break until Sun, Dec 6, 15:00 UTC (episode 7)", delay.message());
        assertEquals("DELAY", delay.kind());
    }

    @Test
    void dubDelayFromAnimeScheduleGoesToDubWatchers() {
        ReleaseTimeEntity row = new ReleaseTimeEntity();
        row.setMediaId(10L);
        row.setAirType("DUB");
        row.setEpisode(5);
        row.setReleaseAt(NOW);
        row.setDelayedUntil(NEW_AT);
        row.setDelayChangedAt(DETECTED);
        row.setDelayAlertPending(true);
        when(releases.findPendingDelayAlerts()).thenReturn(List.of(row));
        when(media.findById(10L)).thenReturn(Optional.of(show(null, null)));
        when(delayAlerts.upsertForFollowers(anyLong(), anyInt(), any(), anyBoolean(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());

        service.alertPending(NOW);

        verify(delayAlerts).upsertForFollowers(eq(10L), eq(5), eq("DELAY_DUB"), eq(true), eq(NEW_AT), eq(DETECTED), eq(NOW),
                eq("Frieren"), eq("Episode 5 (English dub) delayed to Sun, Dec 6, 15:00 UTC"), any());
        verify(releases).clearDelayAlertPending(row.getId(), NEW_AT);
        verifyNoInteractions(push);
    }

    @Test
    void noteThatAlreadyPassedIsClearedWithoutAlerting() {
        MediaEntity passed = show(ScheduleNote.Type.DELAYED, NOW.minusSeconds(1));
        when(media.findPendingDelayAlerts()).thenReturn(List.of(passed));

        assertEquals(0, service.alertPending(NOW));

        verifyNoInteractions(delayAlerts);
        verify(media).clearDelayAlertPending(10L, NOW.minusSeconds(1));
    }

    @Test
    void failedAlertStaysPendingForTheNextRun() {
        when(media.findPendingDelayAlerts()).thenReturn(List.of(show(ScheduleNote.Type.DELAYED, NEW_AT)));
        when(delayAlerts.upsertForFollowers(anyLong(), anyInt(), any(), anyBoolean(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("db hiccup"));

        assertEquals(0, service.alertPending(NOW));

        verify(media, never()).clearDelayAlertPending(any(), any());
    }
}
