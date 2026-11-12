package com.Toine.animeCountdownBackend.services.notifications;

import com.Toine.animeCountdownBackend.repositories.NotificationRepository;
import com.Toine.animeCountdownBackend.repositories.PushSubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Scheduled entry points for episode reminders. Kept apart from ScheduleService (the AniList sync)
 * and never calls AniList: it only reads the schedule the sync has already stored.
 */
@Component
public class EpisodeReminderJob {
    private static final Logger logger = LoggerFactory.getLogger(EpisodeReminderJob.class);

    private final ReminderDispatchService dispatchService;
    private final PushSubscriptionRepository subscriptionRepository;
    private final NotificationRepository notificationRepository;
    private final boolean enabled;
    private final Duration retention;

    public EpisodeReminderJob(ReminderDispatchService dispatchService,
                              PushSubscriptionRepository subscriptionRepository,
                              NotificationRepository notificationRepository,
                              @Value("${app.notifications.enabled:true}") boolean enabled,
                              @Value("${app.notifications.retention:P90D}") Duration retention) {
        this.dispatchService = dispatchService;
        this.subscriptionRepository = subscriptionRepository;
        this.notificationRepository = notificationRepository;
        this.enabled = enabled;
        this.retention = retention;
    }

    @Scheduled(fixedDelayString = "${app.notifications.dispatch.interval:PT30S}",
               initialDelayString = "${app.notifications.dispatch.initial-delay:PT20S}")
    public void sendDueReminders() {
        if (!enabled) {
            return;
        }
        try {
            dispatchService.dispatchDue(Instant.now());
        } catch (RuntimeException e) {
            logger.error("Episode reminder run failed", e);
        }
    }

    /** Daily: drops push subscriptions past their expiration time and old bell entries. */
    @Scheduled(cron = "${app.notifications.cleanup.cron:0 17 4 * * *}")
    public void cleanUp() {
        Instant now = Instant.now();
        int subscriptions = subscriptionRepository.deleteExpired(now);
        int notifications = notificationRepository.deleteOlderThan(now.minus(retention));
        if (subscriptions > 0 || notifications > 0) {
            logger.info("Removed {} expired push subscription(s) and {} old notification(s)", subscriptions, notifications);
        }
    }
}
