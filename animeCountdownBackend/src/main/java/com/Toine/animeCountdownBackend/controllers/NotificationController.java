package com.Toine.animeCountdownBackend.controllers;

import com.Toine.animeCountdownBackend.components.CurrentUser;
import com.Toine.animeCountdownBackend.models.postgreEntities.NotificationEntity;
import com.Toine.animeCountdownBackend.models.postgreEntities.UserEntity;
import com.Toine.animeCountdownBackend.repositories.NotificationRepository;
import com.Toine.animeCountdownBackend.services.notifications.DiscordWebhookService;
import com.Toine.animeCountdownBackend.services.notifications.WebPushService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The notification bell, plus the public switchboard telling the frontend which channels exist. */
@RestController
public class NotificationController {

    public record NotificationDto(Long id, String kind, String title, String message, Long mediaId, Integer episode,
                                  String imageUrl, Instant airingAt, Instant createdAt, boolean read) {
        static NotificationDto of(NotificationEntity n) {
            return new NotificationDto(n.getId(), n.getKind(), n.getTitle(), n.getMessage(), n.getMediaId(),
                    n.getEpisode(), n.getImageUrl(), n.getAiringAt(), n.getNotifDate(), n.getReadAt() != null);
        }
    }

    public record NotificationListDto(List<NotificationDto> items, long unread) {
    }

    private final NotificationRepository notificationRepository;
    private final WebPushService webPushService;
    private final DiscordWebhookService discordWebhookService;

    public NotificationController(NotificationRepository notificationRepository, WebPushService webPushService,
                                  DiscordWebhookService discordWebhookService) {
        this.notificationRepository = notificationRepository;
        this.webPushService = webPushService;
        this.discordWebhookService = discordWebhookService;
    }

    /** Which channels are configured on this server. Public: the UI hides what is unavailable. */
    @GetMapping("/api/notifications/config")
    public Map<String, Object> config() {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("pushEnabled", webPushService.isEnabled());
        config.put("vapidPublicKey", webPushService.getPublicKey());
        config.put("discordEnabled", discordWebhookService.isEnabled());
        return config;
    }

    @GetMapping("/api/auth/notifications")
    public ResponseEntity<NotificationListDto> list(@RequestParam(defaultValue = "30") int limit) {
        UserEntity user = CurrentUser.get();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        int size = Math.max(1, Math.min(limit, 100));
        List<NotificationDto> items = notificationRepository.findRecent(user.getId(), PageRequest.of(0, size))
                .stream().map(NotificationDto::of).toList();
        return ResponseEntity.ok(new NotificationListDto(items, notificationRepository.countUnread(user.getId())));
    }

    @PostMapping("/api/auth/notifications/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long id) {
        UserEntity user = CurrentUser.get();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        notificationRepository.markRead(user.getId(), id, Instant.now());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/auth/notifications/read-all")
    public ResponseEntity<Void> markAllRead() {
        UserEntity user = CurrentUser.get();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        notificationRepository.markAllRead(user.getId(), Instant.now());
        return ResponseEntity.noContent().build();
    }
}
