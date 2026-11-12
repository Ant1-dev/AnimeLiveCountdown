package com.Toine.animeCountdownBackend.services.notifications;

import com.Toine.animeCountdownBackend.models.postgreEntities.NotificationSettingsEntity;
import com.Toine.animeCountdownBackend.models.postgreEntities.ReminderEntity;
import com.Toine.animeCountdownBackend.repositories.NotificationRepository;
import com.Toine.animeCountdownBackend.repositories.NotificationSettingsRepository;
import com.Toine.animeCountdownBackend.repositories.ReminderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Finds reminders that are due and delivers each one once.
 *
 * For every due (user, show, episode) the notification row is inserted first with
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
        List<ReminderCandidate> candidates = reminderRepository.findCandidates(
                selector.windowStart(now), selector.windowEnd(now, ReminderEntity.MAX_LEAD_MINUTES));
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
                logger.error("Reminder for user {} media {} episode {} failed", candidate.userId(),
                        candidate.mediaId(), candidate.episode(), e);
            }
        }
        if (created > 0) {
            logger.info("Sent {} episode reminder(s)", created);
        }
        return created;
    }

    private boolean deliver(ReminderCandidate c, Instant now) {
        String title = c.displayTitle();
        String message = DueEpisodeSelector.describe(c.episode(), c.airingAt(), now);

        int inserted = notificationRepository.insertEpisodeIfAbsent(c.userId(), c.mediaId(), c.episode(),
                c.airingAt(), now, truncate(title, 255), truncate(message, 255), truncate(c.coverImageUrl(), 1024));
        if (inserted == 0) {
            return false; // Already notified about this episode
        }

        String path = "/media/" + c.mediaId();
        if (c.pushEnabled() && webPushService.isEnabled()) {
            // Keep it queued for an offline device until an hour after airing, then let it drop
            Duration ttl = Duration.between(now, c.airingAt().plus(Duration.ofHours(1)));
            if (ttl.compareTo(Duration.ofMinutes(5)) < 0) {
                ttl = Duration.ofMinutes(5);
            }
            webPushService.sendToUser(c.userId(),
                    new PushMessage(title, message, path, c.coverImageUrl(), "m" + c.mediaId() + "e" + c.episode()), ttl);
        }
        if (c.discordEnabled() && discordWebhookService.isEnabled()) {
            sendDiscord(c.userId(), title, message, frontendUrl + path, c.coverImageUrl(), c.airingAt());
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

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() > max ? value.substring(0, max) : value;
    }
}
