package com.Toine.animeCountdownBackend.services.notifications;

import com.Toine.animeCountdownBackend.models.postgreEntities.PushSubscriptionEntity;
import com.Toine.animeCountdownBackend.repositories.PushSubscriptionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.interfaces.ECPrivateKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Sends Web Push messages with VAPID. Disabled (and hidden in the UI) unless both
 * VAPID_PUBLIC_KEY and VAPID_PRIVATE_KEY are set. Subscriptions the push service reports as gone
 * (404/410) are deleted.
 */
@Service
public class WebPushService {
    private static final Logger logger = LoggerFactory.getLogger(WebPushService.class);
    private static final Duration VAPID_TOKEN_LIFETIME = Duration.ofHours(12);

    public enum Result { SENT, GONE, FAILED }

    private final PushSubscriptionRepository subscriptionRepository;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String publicKey;
    private final ECPrivateKey privateKey;
    private final String subject;
    private final List<String> allowedHostSuffixes;

    @Autowired
    public WebPushService(PushSubscriptionRepository subscriptionRepository,
                          @Value("${app.notifications.vapid.public-key:}") String publicKey,
                          @Value("${app.notifications.vapid.private-key:}") String privateKey,
                          @Value("${app.notifications.vapid.subject:https://animelivecountdown.com}") String subject,
                          @Value("${app.notifications.push.allowed-hosts:fcm.googleapis.com,android.googleapis.com,push.services.mozilla.com,push.apple.com,notify.windows.com}") String allowedHosts) {
        this(subscriptionRepository, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(),
                publicKey, privateKey, subject, allowedHosts);
    }

    WebPushService(PushSubscriptionRepository subscriptionRepository, HttpClient httpClient,
                   String publicKey, String privateKey, String subject, String allowedHosts) {
        this.subscriptionRepository = subscriptionRepository;
        this.httpClient = httpClient;
        this.subject = subject;
        this.allowedHostSuffixes = Arrays.stream(allowedHosts.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).map(s -> s.toLowerCase(Locale.ROOT)).toList();

        ECPrivateKey parsedPrivate = null;
        String parsedPublic = null;
        if (!publicKey.isBlank() && !privateKey.isBlank()) {
            try {
                WebPushCrypto.publicKey(WebPushCrypto.b64d(publicKey));
                parsedPrivate = WebPushCrypto.privateKey(WebPushCrypto.b64d(privateKey));
                parsedPublic = WebPushCrypto.b64(WebPushCrypto.b64d(publicKey));
            } catch (GeneralSecurityException | IllegalArgumentException e) {
                logger.error("VAPID keys are invalid; browser push is disabled: {}", e.getMessage());
            }
        }
        this.publicKey = parsedPublic;
        this.privateKey = parsedPrivate;
        if (!isEnabled()) {
            logger.info("Browser push is disabled (VAPID_PUBLIC_KEY / VAPID_PRIVATE_KEY not set)");
        }
    }

    public boolean isEnabled() {
        return publicKey != null && privateKey != null;
    }

    /** The VAPID public key browsers subscribe with, or null when push is disabled. */
    public String getPublicKey() {
        return publicKey;
    }

    /** Whether a subscription endpoint belongs to a known browser push service. */
    public boolean isAllowedEndpoint(String endpoint) {
        try {
            URI uri = URI.create(endpoint);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
                return false;
            }
            String host = uri.getHost().toLowerCase(Locale.ROOT);
            return allowedHostSuffixes.stream().anyMatch(s -> host.equals(s) || host.endsWith("." + s));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** Sends to every subscription of a user. Returns how many deliveries succeeded. */
    public int sendToUser(Long userId, PushMessage message, Duration ttl) {
        if (!isEnabled()) {
            return 0;
        }
        int sent = 0;
        for (PushSubscriptionEntity subscription : subscriptionRepository.findByUser_Id(userId)) {
            if (send(subscription, message, ttl) == Result.SENT) {
                sent++;
            }
        }
        return sent;
    }

    public Result send(PushSubscriptionEntity subscription, PushMessage message, Duration ttl) {
        if (!isEnabled() || !isAllowedEndpoint(subscription.getEndpoint())) {
            return Result.FAILED;
        }
        try {
            byte[] payload = objectMapper.writeValueAsString(message).getBytes(StandardCharsets.UTF_8);
            byte[] body = WebPushCrypto.encrypt(payload,
                    WebPushCrypto.b64d(subscription.getP256dh()), WebPushCrypto.b64d(subscription.getAuth()));

            URI endpoint = URI.create(subscription.getEndpoint());
            String audience = endpoint.getScheme() + "://" + endpoint.getRawAuthority();
            long expires = Instant.now().plus(VAPID_TOKEN_LIFETIME).getEpochSecond();

            HttpRequest.Builder request = HttpRequest.newBuilder(endpoint)
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", WebPushCrypto.vapidAuthorization(audience, subject, expires, privateKey, publicKey))
                    .header("Content-Encoding", "aes128gcm")
                    .header("Content-Type", "application/octet-stream")
                    .header("TTL", Long.toString(Math.max(0, ttl.getSeconds())))
                    .header("Urgency", "high")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body));
            if (message.tag() != null && message.tag().matches("[A-Za-z0-9_-]{1,32}")) {
                request.header("Topic", message.tag());
            }

            HttpResponse<String> response = httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status >= 200 && status < 300) {
                subscription.setLastSuccessAt(Instant.now());
                subscriptionRepository.save(subscription);
                return Result.SENT;
            }
            if (status == 404 || status == 410) {
                logger.info("Push subscription {} is gone ({}); removing it", subscription.getId(), status);
                subscriptionRepository.delete(subscription);
                return Result.GONE;
            }
            logger.warn("Push to subscription {} failed with {}: {}", subscription.getId(), status, truncate(response.body()));
            return Result.FAILED;
        } catch (JsonProcessingException | GeneralSecurityException | IllegalArgumentException e) {
            logger.warn("Could not build push for subscription {}: {}", subscription.getId(), e.getMessage());
            return Result.FAILED;
        } catch (java.io.IOException e) {
            logger.warn("Push to subscription {} failed: {}", subscription.getId(), e.getMessage());
            return Result.FAILED;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.FAILED;
        }
    }

    private static String truncate(String body) {
        if (body == null) {
            return "";
        }
        return body.length() > 200 ? body.substring(0, 200) + "..." : body;
    }
}
