package com.Toine.animeCountdownBackend.services.notifications;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Posts reminders to a Discord channel through a webhook URL the user pasted in.
 *
 * Chosen over bot DMs: a webhook needs no bot token, no shared server and no DM permissions (see
 * docs/notifications-design.md). Off unless DISCORD_WEBHOOK_NOTIFICATIONS_ENABLED=true.
 */
@Service
public class DiscordWebhookService {
    private static final Logger logger = LoggerFactory.getLogger(DiscordWebhookService.class);

    // Only real Discord webhook URLs, so users can't make the server POST anywhere else
    private static final Pattern WEBHOOK_URL = Pattern.compile(
            "^https://(?:(?:canary|ptb)\\.)?discord(?:app)?\\.com/api(?:/v\\d+)?/webhooks/\\d{5,25}/[A-Za-z0-9_-]{20,128}$");
    private static final int EMBED_COLOR = 0x00A6FB;

    public enum Result { SENT, GONE, FAILED }

    private final boolean enabled;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public DiscordWebhookService(@Value("${app.notifications.discord.enabled:false}") boolean enabled) {
        this(enabled, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
    }

    DiscordWebhookService(boolean enabled, HttpClient httpClient) {
        this.enabled = enabled;
        this.httpClient = httpClient;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public static boolean isValidWebhookUrl(String url) {
        return url != null && WEBHOOK_URL.matcher(url.trim()).matches();
    }

    /** "…/webhooks/1234…/abcd…" with the secret token hidden, for showing in settings. */
    public static String mask(String url) {
        if (url == null) {
            return null;
        }
        int lastSlash = url.lastIndexOf('/');
        return lastSlash < 0 ? "configured" : url.substring(0, lastSlash + 1) + "••••••";
    }

    public Result send(String webhookUrl, String title, String description, String link, String imageUrl, Instant timestamp) {
        if (!enabled || !isValidWebhookUrl(webhookUrl)) {
            return Result.FAILED;
        }
        Map<String, Object> embed = new LinkedHashMap<>();
        embed.put("title", truncate(title, 256));
        embed.put("description", truncate(description, 2000));
        if (link != null) {
            embed.put("url", link);
        }
        if (imageUrl != null && imageUrl.startsWith("https://")) {
            embed.put("thumbnail", Map.of("url", imageUrl));
        }
        embed.put("color", EMBED_COLOR);
        if (timestamp != null) {
            embed.put("timestamp", timestamp.toString());
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("username", "Anime Live Countdown");
        body.put("embeds", List.of(embed));
        body.put("allowed_mentions", Map.of("parse", List.of()));

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(webhookUrl.trim()))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            int status = response.statusCode();
            if (status >= 200 && status < 300) {
                return Result.SENT;
            }
            if (status == 401 || status == 403 || status == 404) {
                return Result.GONE;
            }
            logger.warn("Discord webhook answered {}", status);
            return Result.FAILED;
        } catch (java.io.IOException e) {
            logger.warn("Discord webhook failed: {}", e.getMessage());
            return Result.FAILED;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.FAILED;
        }
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() > max ? value.substring(0, max - 1) + "…" : value;
    }
}
