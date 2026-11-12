package com.Toine.animeCountdownBackend.controllers;

import com.Toine.animeCountdownBackend.components.CurrentUser;
import com.Toine.animeCountdownBackend.models.postgreEntities.PushSubscriptionEntity;
import com.Toine.animeCountdownBackend.models.postgreEntities.UserEntity;
import com.Toine.animeCountdownBackend.repositories.PushSubscriptionRepository;
import com.Toine.animeCountdownBackend.repositories.UserRepository;
import com.Toine.animeCountdownBackend.services.notifications.PushMessage;
import com.Toine.animeCountdownBackend.services.notifications.WebPushCrypto;
import com.Toine.animeCountdownBackend.services.notifications.WebPushService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/** Browser push subscriptions for the signed-in user. */
@RestController
@RequestMapping("/api/auth/push")
public class PushSubscriptionController {

    /** The browser's PushSubscription.toJSON(). */
    public record SubscriptionRequest(String endpoint, Long expirationTime, Keys keys) {
        public record Keys(String p256dh, String auth) {
        }
    }

    public record EndpointRequest(String endpoint) {
    }

    private final PushSubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final WebPushService webPushService;

    public PushSubscriptionController(PushSubscriptionRepository subscriptionRepository, UserRepository userRepository,
                                      WebPushService webPushService) {
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository = userRepository;
        this.webPushService = webPushService;
    }

    @PostMapping("/subscribe")
    public ResponseEntity<?> subscribe(@RequestBody SubscriptionRequest request) {
        UserEntity user = CurrentUser.get();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        if (!webPushService.isEnabled()) {
            return ResponseEntity.status(503).body(Map.of("error", "Browser push is not configured"));
        }
        String error = validate(request);
        if (error != null) {
            return ResponseEntity.badRequest().body(Map.of("error", error));
        }

        // Upsert by endpoint: the same browser may re-subscribe or switch accounts
        PushSubscriptionEntity subscription = subscriptionRepository.findByEndpoint(request.endpoint())
                .orElseGet(PushSubscriptionEntity::new);
        subscription.setUser(userRepository.getReferenceById(user.getId()));
        subscription.setEndpoint(request.endpoint());
        subscription.setP256dh(request.keys().p256dh());
        subscription.setAuth(request.keys().auth());
        subscription.setExpirationTime(request.expirationTime() == null ? null : Instant.ofEpochMilli(request.expirationTime()));
        subscriptionRepository.save(subscription);
        return ResponseEntity.ok(Map.of("subscribed", true));
    }

    @PostMapping("/unsubscribe")
    public ResponseEntity<Void> unsubscribe(@RequestBody EndpointRequest request) {
        UserEntity user = CurrentUser.get();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        if (request.endpoint() != null) {
            subscriptionRepository.deleteByUserAndEndpoint(user.getId(), request.endpoint());
        }
        return ResponseEntity.noContent().build();
    }

    /** Sends a test notification to every device the user has subscribed. */
    @PostMapping("/test")
    public ResponseEntity<?> test() {
        UserEntity user = CurrentUser.get();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        if (!webPushService.isEnabled()) {
            return ResponseEntity.status(503).body(Map.of("error", "Browser push is not configured"));
        }
        int sent = webPushService.sendToUser(user.getId(),
                new PushMessage("Test notification", "Episode reminders will look like this.", "/notifications",
                        null, "test"),
                Duration.ofMinutes(10));
        return ResponseEntity.ok(Map.of("sent", sent));
    }

    private String validate(SubscriptionRequest request) {
        if (request.endpoint() == null || request.endpoint().length() > 2048) {
            return "Invalid subscription endpoint";
        }
        if (!webPushService.isAllowedEndpoint(request.endpoint())) {
            return "Unsupported push service";
        }
        if (request.keys() == null || request.keys().p256dh() == null || request.keys().auth() == null) {
            return "Subscription keys are missing";
        }
        try {
            WebPushCrypto.publicKey(WebPushCrypto.b64d(request.keys().p256dh()));
            if (WebPushCrypto.b64d(request.keys().auth()).length != 16) {
                return "Invalid subscription keys";
            }
        } catch (Exception e) {
            return "Invalid subscription keys";
        }
        return null;
    }
}
