package com.Toine.animeCountdownBackend.services.notifications;

import com.Toine.animeCountdownBackend.models.postgreEntities.MediaEntity;
import com.Toine.animeCountdownBackend.models.postgreEntities.NotificationEntity;
import com.Toine.animeCountdownBackend.models.postgreEntities.ReleaseTimeEntity;
import com.Toine.animeCountdownBackend.models.postgreEntities.ReminderEntity;
import com.Toine.animeCountdownBackend.models.postgreEntities.ScheduleNote;
import com.Toine.animeCountdownBackend.repositories.DelayAlertRepository;
import com.Toine.animeCountdownBackend.repositories.MediaRepository;
import com.Toine.animeCountdownBackend.repositories.ReleaseTimeRepository;
import com.Toine.animeCountdownBackend.repositories.ReminderRepository;
import com.Toine.animeCountdownBackend.utils.Slugs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Tells a show's followers when its next episode is delayed or the show goes on break.
 *
 * <p>The syncs only mark what changed: ScheduleNoteDetector flags a media row when AniList pushes
 * the next airing back (a new DELAYED or ON_BREAK note, or its date moving later), and
 * ReleaseTimeSyncService flags a release_time row when AnimeSchedule's delayed_until is newly set
 * or changes. This service picks those flags up, writes one bell entry per follower through
 * {@link DelayAlertRepository} (which also dedupes across runs and across the two sources), sends
 * push and Discord per the follower's reminder for that show, and clears the flag.
 *
 * <p>Japanese-airing and sub delays share the DELAY kind and go to everyone following the show
 * except dub watchers; dub delays (DELAY_DUB) go only to people with a DUB reminder.
 */
@Service
public class DelayAlertService {
    private static final Logger logger = LoggerFactory.getLogger(DelayAlertService.class);
    private static final DateTimeFormatter UTC_DATE =
            DateTimeFormatter.ofPattern("EEE, MMM d, HH:mm 'UTC'", Locale.US).withZone(ZoneOffset.UTC);

    private final MediaRepository mediaRepository;
    private final ReleaseTimeRepository releaseTimeRepository;
    private final ReminderRepository reminderRepository;
    private final DelayAlertRepository delayAlertRepository;
    private final WebPushService webPushService;
    private final DiscordWebhookService discordWebhookService;
    private final ReminderDispatchService dispatchService;
    private final String frontendUrl;

    public DelayAlertService(MediaRepository mediaRepository,
                             ReleaseTimeRepository releaseTimeRepository,
                             ReminderRepository reminderRepository,
                             DelayAlertRepository delayAlertRepository,
                             WebPushService webPushService,
                             DiscordWebhookService discordWebhookService,
                             ReminderDispatchService dispatchService,
                             @Value("${app.frontend.url:https://animelivecountdown.com}") String frontendUrl) {
        this.mediaRepository = mediaRepository;
        this.releaseTimeRepository = releaseTimeRepository;
        this.reminderRepository = reminderRepository;
        this.delayAlertRepository = delayAlertRepository;
        this.webPushService = webPushService;
        this.discordWebhookService = discordWebhookService;
        this.dispatchService = dispatchService;
        this.frontendUrl = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
    }

    /** One alert to send: a show's episode moved to {@code newAt}. */
    record Delay(long mediaId, int episode, String kind, boolean dubAudience, Instant newAt, Instant followedBy,
                 String title, String imageUrl, String message, String discordMessage) {
    }

    /** Alerts every pending delay. Returns how many followers were newly alerted. */
    public int alertPending(Instant now) {
        int alerted = 0;
        for (MediaEntity media : mediaRepository.findPendingDelayAlerts()) {
            try {
                Delay delay = fromScheduleNote(media, now);
                if (delay != null) {
                    alerted += alert(delay, now);
                }
                mediaRepository.clearDelayAlertPending(media.getId(), media.getScheduleNoteExpiresAt());
            } catch (RuntimeException e) {
                logger.error("Delay alert for media {} failed; retrying next run", media.getId(), e);
            }
        }
        for (ReleaseTimeEntity row : releaseTimeRepository.findPendingDelayAlerts()) {
            try {
                Delay delay = fromReleaseTime(row, now);
                if (delay != null) {
                    alerted += alert(delay, now);
                }
                releaseTimeRepository.clearDelayAlertPending(row.getId(), row.getDelayedUntil());
            } catch (RuntimeException e) {
                logger.error("Delay alert for release {} failed; retrying next run", row.getId(), e);
            }
        }
        if (alerted > 0) {
            logger.info("Sent {} delay alert(s)", alerted);
        }
        return alerted;
    }

    /** From an AniList break/delay note, or null if there's nothing (left) to alert about. */
    Delay fromScheduleNote(MediaEntity media, Instant now) {
        ScheduleNote.Type type = media.getScheduleNoteType();
        Instant newAt = media.getScheduleNoteExpiresAt();
        Integer episode = media.getNext_Airing_Episode();
        if (type == null || newAt == null || !newAt.isAfter(now) || episode == null) {
            return null;
        }
        String message = type == ScheduleNote.Type.ON_BREAK
                ? breakText(episode, UTC_DATE.format(newAt))
                : delayText(episode, null, UTC_DATE.format(newAt));
        String discordMessage = type == ScheduleNote.Type.ON_BREAK
                ? breakText(episode, discordDate(newAt))
                : delayText(episode, null, discordDate(newAt));
        Instant followedBy = media.getScheduleNoteDetectedAt() != null ? media.getScheduleNoteDetectedAt() : now;
        return new Delay(media.getId(), episode, NotificationEntity.KIND_DELAY, false, newAt, followedBy,
                displayTitle(media), media.getCover_Image_Url(), message, discordMessage);
    }

    /** From an AnimeSchedule delayed_until, or null if there's nothing (left) to alert about. */
    Delay fromReleaseTime(ReleaseTimeEntity row, Instant now) {
        Instant newAt = row.getDelayedUntil();
        if (newAt == null || !newAt.isAfter(now) || row.getEpisode() == null) {
            return null;
        }
        MediaEntity media = mediaRepository.findById(row.getMediaId()).orElse(null);
        if (media == null) {
            return null; // Not a show anyone can follow here
        }
        boolean dub = ReminderEntity.TYPE_DUB.equals(row.getAirType());
        String version = DueEpisodeSelector.versionLabel(row.getAirType());
        Instant followedBy = row.getDelayChangedAt() != null ? row.getDelayChangedAt() : now;
        return new Delay(row.getMediaId(), row.getEpisode(),
                dub ? NotificationEntity.KIND_DELAY_DUB : NotificationEntity.KIND_DELAY, dub, newAt, followedBy,
                displayTitle(media), media.getCover_Image_Url(),
                delayText(row.getEpisode(), version, UTC_DATE.format(newAt)),
                delayText(row.getEpisode(), version, discordDate(newAt)));
    }

    private int alert(Delay d, Instant now) {
        List<Long> users = delayAlertRepository.upsertForFollowers(d.mediaId(), d.episode(), d.kind(), d.dubAudience(),
                d.newAt(), d.followedBy(), now, truncate(d.title(), 255), truncate(d.message(), 255),
                truncate(d.imageUrl(), 1024));
        if (users.isEmpty()) {
            return 0;
        }
        String path = Slugs.animePath(d.mediaId(), d.title());
        for (ReminderChannels reminder : reminderRepository.findChannels(d.mediaId(), users)) {
            Long userId = reminder.userId();
            try {
                if (reminder.pushEnabled() && webPushService.isEnabled()) {
                    webPushService.sendToUser(userId, new PushMessage(d.title(), d.message(), path, d.imageUrl(),
                            "m" + d.mediaId() + "e" + d.episode() + "delay"), Duration.ofHours(12));
                }
                if (reminder.discordEnabled() && discordWebhookService.isEnabled()) {
                    dispatchService.sendDiscord(userId, d.title(), d.discordMessage(), frontendUrl + path,
                            d.imageUrl(), now);
                }
            } catch (RuntimeException e) {
                logger.warn("Delay alert delivery to user {} for media {} failed: {}", userId, d.mediaId(), e.getMessage());
            }
        }
        return users.size();
    }

    static String delayText(int episode, String version, String date) {
        return "Episode " + episode + (version == null ? "" : " (" + version + ")") + " delayed to " + date;
    }

    static String breakText(int episode, String date) {
        return "On break until " + date + " (episode " + episode + ")";
    }

    /** Discord renders this in each reader's own time zone. */
    private static String discordDate(Instant at) {
        return "<t:" + at.getEpochSecond() + ":F>";
    }

    private static String displayTitle(MediaEntity media) {
        if (media.getTitle_English() != null && !media.getTitle_English().isBlank()) {
            return media.getTitle_English();
        }
        return media.getTitle_Romaji() != null ? media.getTitle_Romaji() : "Unknown title";
    }

    private static String truncate(String value, int max) {
        return ReminderDispatchService.truncate(value, max);
    }
}
