package com.Toine.animeCountdownBackend.services.notifications;

import com.Toine.animeCountdownBackend.models.postgreEntities.NotificationSettingsEntity;
import com.Toine.animeCountdownBackend.repositories.NotificationRepository;
import com.Toine.animeCountdownBackend.repositories.NotificationSettingsRepository;
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

class ReminderDispatchServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-20T14:45:00Z");
    private static final String WEBHOOK = "https://discord.com/api/webhooks/123456789012/abcdefghijklmnopqrstuvwxyz";

    private ReminderRepository reminders;
    private NotificationRepository notifications;
    private NotificationSettingsRepository settings;
    private WebPushService push;
    private DiscordWebhookService discord;
    private ReminderDispatchService service;

    @BeforeEach
    void setUp() {
        reminders = mock(ReminderRepository.class);
        notifications = mock(NotificationRepository.class);
        settings = mock(NotificationSettingsRepository.class);
        push = mock(WebPushService.class);
        discord = mock(DiscordWebhookService.class);
        when(push.isEnabled()).thenReturn(true);
        when(discord.isEnabled()).thenReturn(true);
        service = new ReminderDispatchService(reminders, notifications, settings, push, discord,
                Duration.ofSeconds(30), Duration.ofMinutes(15), "https://animelivecountdown.com/");
    }

    private static ReminderCandidate candidate(long userId, long mediaId, int lead, Instant airs, boolean push, boolean discord) {
        return new ReminderCandidate(userId, mediaId, lead, push, discord, airs, 5, "Frieren", "Sousou no Frieren", "https://img/1.png");
    }

    @Test
    void sendsDueReminderOnceOnEveryChannel() {
        ReminderCandidate due = candidate(1L, 10L, 15, NOW.plus(Duration.ofMinutes(15)), true, true);
        when(reminders.findCandidates(any(), any())).thenReturn(List.of(due));
        when(notifications.insertEpisodeIfAbsent(eq(1L), eq(10L), eq(5), any(), eq(NOW), any(), any(), any(), any(), any())).thenReturn(1);
        NotificationSettingsEntity userSettings = new NotificationSettingsEntity();
        userSettings.setDiscordWebhookUrl(WEBHOOK);
        when(settings.findById(1L)).thenReturn(Optional.of(userSettings));
        when(discord.send(any(), any(), any(), any(), any(), any())).thenReturn(DiscordWebhookService.Result.SENT);

        assertEquals(1, service.dispatchDue(NOW));

        ArgumentCaptor<PushMessage> message = ArgumentCaptor.forClass(PushMessage.class);
        verify(push).sendToUser(eq(1L), message.capture(), any());
        assertEquals("Frieren", message.getValue().title());
        assertEquals("Episode 5 airs in 15 minutes", message.getValue().body());
        assertEquals("/anime/10/frieren", message.getValue().url());
        assertEquals("m10e5", message.getValue().tag());
        verify(discord).send(eq(WEBHOOK), eq("Frieren"), eq("Episode 5 airs in 15 minutes"),
                eq("https://animelivecountdown.com/anime/10/frieren"), eq("https://img/1.png"), any());
    }

    @Test
    void alreadyNotifiedEpisodeIsNotSentAgain() {
        ReminderCandidate due = candidate(1L, 10L, 0, NOW.plusSeconds(10), true, true);
        when(reminders.findCandidates(any(), any())).thenReturn(List.of(due));
        // The unique (user, media, episode) row exists from an earlier run
        when(notifications.insertEpisodeIfAbsent(anyLong(), anyLong(), anyInt(), any(), any(), any(), any(), any(), any(), any())).thenReturn(0);

        assertEquals(0, service.dispatchDue(NOW));

        verify(push, never()).sendToUser(any(), any(), any());
        verifyNoInteractions(discord);
    }

    @Test
    void repeatedRunsDeliverEachEpisodeOnce() {
        ReminderCandidate due = candidate(1L, 10L, 0, NOW.plusSeconds(10), true, false);
        when(reminders.findCandidates(any(), any())).thenReturn(List.of(due));
        when(notifications.insertEpisodeIfAbsent(anyLong(), anyLong(), anyInt(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(1).thenReturn(0);

        service.dispatchDue(NOW);
        service.dispatchDue(NOW.plusSeconds(30));

        verify(push, times(1)).sendToUser(eq(1L), any(), any());
    }

    @Test
    void remindersThatAreNotDueYetAreSkipped() {
        ReminderCandidate later = candidate(1L, 10L, 15, NOW.plus(Duration.ofHours(3)), true, true);
        when(reminders.findCandidates(any(), any())).thenReturn(List.of(later));

        assertEquals(0, service.dispatchDue(NOW));

        verify(notifications, never()).insertEpisodeIfAbsent(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        verifyNoInteractions(discord);
    }

    @Test
    void channelsFollowThePreferenceAndServerConfig() {
        ReminderCandidate pushOnly = candidate(1L, 10L, 0, NOW.plusSeconds(10), true, false);
        ReminderCandidate discordOnly = candidate(2L, 11L, 0, NOW.plusSeconds(10), false, true);
        when(reminders.findCandidates(any(), any())).thenReturn(List.of(pushOnly, discordOnly));
        when(notifications.insertEpisodeIfAbsent(anyLong(), anyLong(), anyInt(), any(), any(), any(), any(), any(), any(), any())).thenReturn(1);
        when(discord.isEnabled()).thenReturn(false);

        assertEquals(2, service.dispatchDue(NOW));

        verify(push).sendToUser(eq(1L), any(), any());
        verify(push, never()).sendToUser(eq(2L), any(), any());
        verify(discord, never()).send(any(), any(), any(), any(), any(), any());
    }

    @Test
    void goneDiscordWebhookIsForgotten() {
        ReminderCandidate due = candidate(1L, 10L, 0, NOW.plusSeconds(10), false, true);
        when(reminders.findCandidates(any(), any())).thenReturn(List.of(due));
        when(notifications.insertEpisodeIfAbsent(anyLong(), anyLong(), anyInt(), any(), any(), any(), any(), any(), any(), any())).thenReturn(1);
        NotificationSettingsEntity userSettings = new NotificationSettingsEntity();
        userSettings.setDiscordWebhookUrl(WEBHOOK);
        when(settings.findById(1L)).thenReturn(Optional.of(userSettings));
        when(discord.send(any(), any(), any(), any(), any(), any())).thenReturn(DiscordWebhookService.Result.GONE);

        service.dispatchDue(NOW);

        assertNull(userSettings.getDiscordWebhookUrl());
        verify(settings).save(userSettings);
    }

    @Test
    void oneFailingReminderDoesNotStopTheOthers() {
        ReminderCandidate broken = candidate(1L, 10L, 0, NOW.plusSeconds(10), true, false);
        ReminderCandidate fine = candidate(2L, 11L, 0, NOW.plusSeconds(10), true, false);
        when(reminders.findCandidates(any(), any())).thenReturn(List.of(broken, fine));
        when(notifications.insertEpisodeIfAbsent(eq(1L), anyLong(), anyInt(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("db hiccup"));
        when(notifications.insertEpisodeIfAbsent(eq(2L), anyLong(), anyInt(), any(), any(), any(), any(), any(), any(), any())).thenReturn(1);

        assertEquals(1, service.dispatchDue(NOW));
        verify(push).sendToUser(eq(2L), any(), any());
    }

    private static ReminderCandidate release(long userId, String type, Instant releaseAt, Instant delayedUntil, String streams) {
        return new ReminderCandidate(userId, 10L, 0, true, true, releaseAt, delayedUntil, 5, "Frieren",
                "Sousou no Frieren", "https://img/1.png", type, streams);
    }

    @Test
    void dubReminderSaysWhichVersionAndLinksThePlatform() {
        ReminderCandidate dub = release(1L, "DUB", NOW.plusSeconds(10), null,
                "{\"hidive\":\"https://hidive.com/f\",\"crunchyroll\":\"https://crunchyroll.com/f\"}");
        when(reminders.findReleaseCandidates(any(), any())).thenReturn(List.of(dub));
        when(notifications.insertEpisodeIfAbsent(anyLong(), anyLong(), anyInt(), any(), any(), any(), any(), any(), any(), any())).thenReturn(1);
        NotificationSettingsEntity userSettings = new NotificationSettingsEntity();
        userSettings.setDiscordWebhookUrl(WEBHOOK);
        when(settings.findById(1L)).thenReturn(Optional.of(userSettings));
        when(discord.send(any(), any(), any(), any(), any(), any())).thenReturn(DiscordWebhookService.Result.SENT);

        assertEquals(1, service.dispatchDue(NOW));

        verify(notifications).insertEpisodeIfAbsent(eq(1L), eq(10L), eq(5), any(), eq(NOW), eq("Frieren"),
                eq("Episode 5 (English dub) is out now on Crunchyroll"), any(), eq("EPISODE_DUB"),
                eq("https://crunchyroll.com/f"));
        ArgumentCaptor<PushMessage> message = ArgumentCaptor.forClass(PushMessage.class);
        verify(push).sendToUser(eq(1L), message.capture(), any());
        assertEquals("m10e5d", message.getValue().tag());
        assertEquals("/anime/10/frieren", message.getValue().url());
        verify(discord).send(eq(WEBHOOK), eq("Frieren"),
                eq("Episode 5 (English dub) is out now on Crunchyroll\n[Watch on Crunchyroll](https://crunchyroll.com/f)"),
                eq("https://animelivecountdown.com/anime/10/frieren"), any(), any());
    }

    @Test
    void subReminderWithoutStreamsHasNoLink() {
        ReminderCandidate sub = release(1L, "SUB", NOW.plus(Duration.ofMinutes(30)), null, null);
        ReminderCandidate early = new ReminderCandidate(1L, 10L, 30, true, false, sub.airingAt(), null, 5,
                "Frieren", null, null, "SUB", null);
        when(reminders.findReleaseCandidates(any(), any())).thenReturn(List.of(early));
        when(notifications.insertEpisodeIfAbsent(anyLong(), anyLong(), anyInt(), any(), any(), any(), any(), any(), any(), any())).thenReturn(1);

        assertEquals(1, service.dispatchDue(NOW));

        verify(notifications).insertEpisodeIfAbsent(eq(1L), eq(10L), eq(5), any(), any(), any(),
                eq("Episode 5 (subbed) is out in 30 minutes"), any(), eq("EPISODE_SUB"), isNull());
    }

    @Test
    void delayedReleaseIsDueAtItsDelayedTime() {
        // Listed as out now, but AnimeSchedule says it's delayed by three hours
        ReminderCandidate delayed = release(1L, "SUB", NOW, NOW.plus(Duration.ofHours(3)), null);
        when(reminders.findReleaseCandidates(any(), any())).thenReturn(List.of(delayed));

        assertEquals(NOW.plus(Duration.ofHours(3)), delayed.airingAt());
        assertEquals(0, service.dispatchDue(NOW));
        verify(notifications, never()).insertEpisodeIfAbsent(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void japanAndDubRemindersForTheSameEpisodeUseSeparateKinds() {
        ReminderCandidate japan = candidate(1L, 10L, 0, NOW.plusSeconds(10), false, false);
        ReminderCandidate dub = new ReminderCandidate(2L, 10L, 0, false, false, NOW.plusSeconds(10), null, 5,
                "Frieren", null, null, "DUB", null);
        when(reminders.findCandidates(any(), any())).thenReturn(List.of(japan));
        when(reminders.findReleaseCandidates(any(), any())).thenReturn(List.of(dub));
        when(notifications.insertEpisodeIfAbsent(anyLong(), anyLong(), anyInt(), any(), any(), any(), any(), any(), any(), any())).thenReturn(1);

        assertEquals(2, service.dispatchDue(NOW));

        verify(notifications).insertEpisodeIfAbsent(eq(1L), eq(10L), eq(5), any(), any(), any(),
                eq("Episode 5 is out now"), any(), eq("EPISODE"), isNull());
        verify(notifications).insertEpisodeIfAbsent(eq(2L), eq(10L), eq(5), any(), any(), any(),
                eq("Episode 5 (English dub) is out now"), any(), eq("EPISODE_DUB"), isNull());
    }
}
