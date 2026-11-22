package com.Toine.animeCountdownBackend.services.notifications;

import com.Toine.animeCountdownBackend.utils.Slugs;
import com.Toine.animeCountdownBackend.models.postgreEntities.NotificationEntity;
import com.Toine.animeCountdownBackend.models.postgreEntities.NotificationSettingsEntity;
import com.Toine.animeCountdownBackend.models.postgreEntities.ReminderEntity;
import com.Toine.animeCountdownBackend.repositories.NotificationRepository;
import com.Toine.animeCountdownBackend.repositories.NotificationSettingsRepository;
import com.Toine.animeCountdownBackend.repositories.ReminderRepository;
import com.Toine.animeCountdownBackend.services.releases.ReleaseTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Finds reminders that are due and delivers each one once.
 *
 * JAPAN reminders follow AniList's next airing; SUB and DUB reminders follow the matching
 * release_time rows synced from AnimeSchedule. For every due (user, show, episode, kind) the
 * notification row is inserted first with
 * ON CONFLICT DO NOTHING; push and Discord are only sent when that insert created the row. The
 * database's unique key is therefore the dedupe, and a crash between insert and send loses at most
 * one delivery instead of repeating it.
 */
@Service
public class ReminderDispatchService {
    private static final Logger logger = LoggerFactory.getLogger(ReminderDispatchService.class);

    private final ReminderRepository reminderRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationSettingsRepository settingsRepository;
    private final WebPushService webPushService;
    private final DiscordWebhookService discordWebhookService;
    private final DueEpisodeSelector selector;
    private final String frontendUrl;

    public ReminderDispatchService(ReminderRepository reminderRepository,
                                   NotificationRepository notificationRepository,
                                   NotificationSettingsRepository settingsRepository,
                                   WebPushService webPushService,
                                   DiscordWebhookService discordWebhookService,
                                   @Value("${app.notifications.dispatch.interval:PT30S}") Duration interval,
                                   @Value("${app.notifications.dispatch.grace:PT15M}") Duration grace,
                                   @Value("${app.frontend.url:https://animelivecountdown.com}") String frontendUrl) {
        this.reminderRepository = reminderRepository;
        this.notificationRepository = notificationRepository;
        this.settingsRepository = settingsRepository;
        this.webPushService = webPushService;
        this.discordWebhookService = discordWebhookService;
        this.selector = new DueEpisodeSelector(interval, grace);
        this.frontendUrl = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
    }

    /** Sends every reminder due at {@code now}. Returns how many new notifications were created. */
    public int dispatchDue(Instant now) {
        Instant from = selector.windowStart(now);
        Instant to = selector.windowEnd(now, ReminderEntity.MAX_LEAD_MINUTES);
        List<ReminderCandidate> candidates = new ArrayList<>(reminderRepository.findCandidates(from, to));
        candidates.addAll(reminderRepository.findReleaseCandidates(from, to));
        int created = 0;
        for (ReminderCandidate candidate : candidates) {
            if (!selector.isDue(candidate, now)) {
                continue;
            }
            try {
                if (deliver(candidate, now)) {
                    created++;
                }
            } catch (RuntimeException e) {
                logger.error("{} reminder for user {} media {} episode {} failed", candidate.releaseType(),
                        candidate.userId(), candidate.mediaId(), candidate.episode(), e);
            }
        }
        if (created > 0) {
            logger.info("Sent {} episode reminder(s)", created);
        }
        return created;
    }

    private boolean deliver(ReminderCandidate c, Instant now) {
        String title = c.displayTitle();
        ReleaseTimes.Stream stream = ReleaseTimes.primaryStream(c.streamsJson()).orElse(null);
        String message = DueEpisodeSelector.describe(c.episode(), c.airingAt(), now,
                DueEpisodeSelector.versionLabel(c.releaseType()), stream == null ? null : stream.label());
        String kind = NotificationEntity.episodeKind(c.releaseType());
        String streamUrl = stream == null ? null : truncate(stream.url(), 1024);

        int inserted = notificationRepository.insertEpisodeIfAbsent(c.userId(), c.mediaId(), c.episode(),
                c.airingAt(), now, truncate(title, 255), truncate(message, 255), truncate(c.coverImageUrl(), 1024),
                kind, streamUrl);
        if (inserted == 0) {
            return false; // Already notified about this episode in this version
        }

        String path = Slugs.animePath(c.mediaId(), title);
        if (c.pushEnabled() && webPushService.isEnabled()) {
            // Keep it queued for an offline device until an hour after airing, then let it drop
            Duration ttl = Duration.between(now, c.airingAt().plus(Duration.ofHours(1)));
            if (ttl.compareTo(Duration.ofMinutes(5)) < 0) {
                ttl = Duration.ofMinutes(5);
            }
            String tag = "m" + c.mediaId() + "e" + c.episode() + switch (kind) {
                case NotificationEntity.KIND_EPISODE_SUB -> "s";
                case NotificationEntity.KIND_EPISODE_DUB -> "d";
                default -> "";
            };
            webPushService.sendToUser(c.userId(), new PushMessage(title, message, path, c.coverImageUrl(), tag), ttl);
        }
        if (c.discordEnabled() && discordWebhookService.isEnabled()) {
            String description = stream == null ? message
                    : message + "\n[Watch on " + stream.label() + "](" + stream.url() + ")";
            sendDiscord(c.userId(), title, description, frontendUrl + path, c.coverImageUrl(), c.airingAt());
        }
        return true;
    }

    /** Posts to the user's webhook, forgetting it if Discord says it no longer exists. */
    public DiscordWebhookService.Result sendDiscord(Long userId, String title, String message, String link,
                                                    String imageUrl, Instant timestamp) {
        Optional<NotificationSettingsEntity> settings = settingsRepository.findById(userId);
        String webhook = settings.map(NotificationSettingsEntity::getDiscordWebhookUrl).orElse(null);
        if (webhook == null) {
            return DiscordWebhookService.Result.FAILED;
        }
        DiscordWebhookService.Result result = discordWebhookService.send(webhook, title, message, link, imageUrl, timestamp);
        if (result == DiscordWebhookService.Result.GONE) {
            logger.info("Discord webhook for user {} is gone; removing it", userId);
            settings.get().setDiscordWebhookUrl(null);
            settingsRepository.save(settings.get());
        }
        return result;
    }

    static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() > max ? value.substring(0, max) : value;
    }
}
