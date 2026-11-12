package com.Toine.animeCountdownBackend.controllers;

import com.Toine.animeCountdownBackend.components.CurrentUser;
import com.Toine.animeCountdownBackend.models.postgreEntities.NotificationSettingsEntity;
import com.Toine.animeCountdownBackend.models.postgreEntities.UserEntity;
import com.Toine.animeCountdownBackend.repositories.NotificationSettingsRepository;
import com.Toine.animeCountdownBackend.services.notifications.CalendarFeedService;
import com.Toine.animeCountdownBackend.services.notifications.DiscordWebhookService;
import com.Toine.animeCountdownBackend.services.notifications.ReminderDispatchService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/** Channel settings: Discord webhook and the secret calendar feed URL. */
@RestController
@RequestMapping("/api/auth")
public class NotificationSettingsController {

    public record WebhookRequest(String webhookUrl) {
    }

    private final NotificationSettingsRepository settingsRepository;
    private final CalendarFeedService calendarFeedService;
    private final DiscordWebhookService discordWebhookService;
    private final ReminderDispatchService dispatchService;

    public NotificationSettingsController(NotificationSettingsRepository settingsRepository,
                                          CalendarFeedService calendarFeedService,
                                          DiscordWebhookService discordWebhookService,
                                          ReminderDispatchService dispatchService) {
        this.settingsRepository = settingsRepository;
        this.calendarFeedService = calendarFeedService;
        this.discordWebhookService = discordWebhookService;
        this.dispatchService = dispatchService;
    }

    @GetMapping("/notification-settings")
    public ResponseEntity<Map<String, Object>> get(HttpServletRequest request) {
        UserEntity user = CurrentUser.get();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        NotificationSettingsEntity settings = settingsRepository.findById(user.getId()).orElse(null);
        String webhook = settings != null ? settings.getDiscordWebhookUrl() : null;
        String token = settings != null ? settings.getCalendarToken() : null;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("discordEnabled", discordWebhookService.isEnabled());
        body.put("discordWebhookSet", webhook != null);
        body.put("discordWebhookHint", DiscordWebhookService.mask(webhook));
        body.putAll(calendarUrls(token));
        return ResponseEntity.ok(body);
    }

    @PutMapping("/notification-settings/discord")
    public ResponseEntity<?> setDiscord(@RequestBody WebhookRequest request) {
        UserEntity user = CurrentUser.get();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        if (!discordWebhookService.isEnabled()) {
            return ResponseEntity.status(503).body(Map.of("error", "Discord notifications are not enabled"));
        }
        if (!DiscordWebhookService.isValidWebhookUrl(request.webhookUrl())) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    "That doesn't look like a Discord webhook URL (https://discord.com/api/webhooks/...)"));
        }
        NotificationSettingsEntity settings = calendarFeedService.settingsFor(user.getId());
        settings.setDiscordWebhookUrl(request.webhookUrl().trim());
        settingsRepository.save(settings);
        return ResponseEntity.ok(Map.of("discordWebhookSet", true,
                "discordWebhookHint", DiscordWebhookService.mask(settings.getDiscordWebhookUrl())));
    }

    @DeleteMapping("/notification-settings/discord")
    public ResponseEntity<Void> removeDiscord() {
        UserEntity user = CurrentUser.get();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        settingsRepository.findById(user.getId()).ifPresent(settings -> {
            settings.setDiscordWebhookUrl(null);
            settingsRepository.save(settings);
        });
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/notification-settings/discord/test")
    public ResponseEntity<?> testDiscord() {
        UserEntity user = CurrentUser.get();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        if (!discordWebhookService.isEnabled()) {
            return ResponseEntity.status(503).body(Map.of("error", "Discord notifications are not enabled"));
        }
        DiscordWebhookService.Result result = dispatchService.sendDiscord(user.getId(), "Test notification",
                "Episode reminders from Anime Live Countdown will look like this.", null, null, Instant.now());
        return switch (result) {
            case SENT -> ResponseEntity.ok(Map.of("sent", true));
            case GONE -> ResponseEntity.status(410).body(Map.of("error", "Discord says this webhook no longer exists; it was removed"));
            case FAILED -> ResponseEntity.status(502).body(Map.of("error", "Discord didn't accept the message; check the webhook URL"));
        };
    }

    @PostMapping("/calendar/token")
    public ResponseEntity<Map<String, Object>> regenerateCalendarToken() {
        UserEntity user = CurrentUser.get();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(calendarUrls(calendarFeedService.regenerateToken(user.getId())));
    }

    @DeleteMapping("/calendar/token")
    public ResponseEntity<Void> disableCalendar() {
        UserEntity user = CurrentUser.get();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        calendarFeedService.disable(user.getId());
        return ResponseEntity.noContent().build();
    }

    /** Feed URLs on this API's own public origin (X-Forwarded-* aware). */
    private static Map<String, Object> calendarUrls(String token) {
        Map<String, Object> urls = new LinkedHashMap<>();
        if (token == null) {
            urls.put("calendarUrl", null);
            urls.put("calendarWebcalUrl", null);
            return urls;
        }
        String https = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/calendar/{token}.ics").buildAndExpand(token).toUriString();
        urls.put("calendarUrl", https);
        urls.put("calendarWebcalUrl", https.replaceFirst("^https?://", "webcal://"));
        return urls;
    }
}
