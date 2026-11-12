package com.Toine.animeCountdownBackend.services.notifications;

import com.Toine.animeCountdownBackend.models.postgreEntities.PushSubscriptionEntity;
import com.Toine.animeCountdownBackend.repositories.PushSubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyPair;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.time.Duration;
import java.util.List;

import static com.Toine.animeCountdownBackend.services.notifications.WebPushCrypto.b64;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WebPushServiceTest {

    private static final String HOSTS = "fcm.googleapis.com,push.services.mozilla.com,push.apple.com";

    private PushSubscriptionRepository repository;
    private HttpClient http;
    private WebPushService service;

    @BeforeEach
    void setUp() throws Exception {
        repository = mock(PushSubscriptionRepository.class);
        http = mock(HttpClient.class);
        KeyPair vapid = WebPushCrypto.generateKeyPair();
        service = new WebPushService(repository, http,
                b64(WebPushCrypto.encodePublic((ECPublicKey) vapid.getPublic())),
                b64(WebPushCrypto.encodePrivate((ECPrivateKey) vapid.getPrivate())),
                "https://animelivecountdown.com", HOSTS);
    }

    private static PushSubscriptionEntity subscription(String endpoint) throws Exception {
        PushSubscriptionEntity s = new PushSubscriptionEntity();
        s.setEndpoint(endpoint);
        s.setP256dh(b64(WebPushCrypto.encodePublic((ECPublicKey) WebPushCrypto.generateKeyPair().getPublic())));
        s.setAuth("BTBZMqHH6r4Tts7J_aSIgg");
        return s;
    }

    @SuppressWarnings("unchecked")
    private void respond(int status) throws Exception {
        HttpResponse<Object> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(http.send(any(), any())).thenReturn(response);
    }

    @Test
    void disabledWithoutKeys() {
        WebPushService off = new WebPushService(repository, http, "", "", "https://x", HOSTS);
        assertFalse(off.isEnabled());
        assertNull(off.getPublicKey());
        assertEquals(0, off.sendToUser(1L, new PushMessage("t", "b", "/", null, null), Duration.ofMinutes(1)));
        verifyNoInteractions(repository);
    }

    @Test
    void sendsEncryptedRequestWithVapidHeaders() throws Exception {
        respond(201);
        PushSubscriptionEntity sub = subscription("https://fcm.googleapis.com/fcm/send/abc");

        assertEquals(WebPushService.Result.SENT, service.send(sub, new PushMessage("t", "b", "/", null, "m1e2"), Duration.ofMinutes(5)));

        ArgumentCaptor<HttpRequest> request = ArgumentCaptor.forClass(HttpRequest.class);
        verify(http).send(request.capture(), any());
        HttpRequest sent = request.getValue();
        assertEquals("aes128gcm", sent.headers().firstValue("Content-Encoding").orElseThrow());
        assertEquals("300", sent.headers().firstValue("TTL").orElseThrow());
        assertEquals("m1e2", sent.headers().firstValue("Topic").orElseThrow());
        assertTrue(sent.headers().firstValue("Authorization").orElseThrow().startsWith("vapid t="));
        assertNotNull(sub.getLastSuccessAt());
        verify(repository).save(sub);
    }

    @Test
    void expiredSubscriptionsAreDeleted() throws Exception {
        respond(410);
        PushSubscriptionEntity gone = subscription("https://updates.push.services.mozilla.com/wpush/v2/abc");
        when(repository.findByUser_Id(1L)).thenReturn(List.of(gone));

        assertEquals(0, service.sendToUser(1L, new PushMessage("t", "b", "/", null, null), Duration.ofMinutes(5)));

        verify(repository).delete(gone);
    }

    @Test
    void otherFailuresKeepTheSubscription() throws Exception {
        respond(500);
        PushSubscriptionEntity sub = subscription("https://web.push.apple.com/QH8");

        assertEquals(WebPushService.Result.FAILED, service.send(sub, new PushMessage("t", "b", "/", null, null), Duration.ofMinutes(5)));
        verify(repository, never()).delete(any());
    }

    @Test
    void onlyKnownPushServicesAreAllowed() throws Exception {
        assertTrue(service.isAllowedEndpoint("https://fcm.googleapis.com/fcm/send/abc"));
        assertTrue(service.isAllowedEndpoint("https://web.push.apple.com/abc"));
        assertFalse(service.isAllowedEndpoint("http://fcm.googleapis.com/fcm/send/abc"));
        assertFalse(service.isAllowedEndpoint("https://evil-fcm.googleapis.com.attacker.net/x"));
        assertFalse(service.isAllowedEndpoint("https://169.254.169.254/latest"));
        assertFalse(service.isAllowedEndpoint("not a url"));

        assertEquals(WebPushService.Result.FAILED,
                service.send(subscription("https://internal.example/x"), new PushMessage("t", "b", "/", null, null), Duration.ofMinutes(1)));
        verifyNoInteractions(http);
    }
}
