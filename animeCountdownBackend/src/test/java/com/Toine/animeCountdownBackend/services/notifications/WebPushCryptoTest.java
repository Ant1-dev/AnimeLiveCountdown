package com.Toine.animeCountdownBackend.services.notifications;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.Signature;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.Base64;

import static com.Toine.animeCountdownBackend.services.notifications.WebPushCrypto.b64;
import static com.Toine.animeCountdownBackend.services.notifications.WebPushCrypto.b64d;
import static org.junit.jupiter.api.Assertions.*;

class WebPushCryptoTest {

    // RFC 8291 section 5 / appendix A test vector
    private static final String PLAINTEXT = "When I grow up, I want to be a watermelon";
    private static final String AS_PRIVATE = "yfWPiYE-n46HLnH0KqZOF1fJJU3MYrct3AELtAQ-oRw";
    private static final String AS_PUBLIC = "BP4z9KsN6nGRTbVYI_c7VJSPQTBtkgcy27mlmlMoZIIgDll6e3vCYLocInmYWAmS6TlzAC8wEqKK6PBru3jl7A8";
    private static final String UA_PUBLIC = "BCVxsr7N_eNgVRqvHtD0zTZsEc6-VV-JvLexhqUzORcxaOzi6-AYWXvTBHm4bjyPjs7Vd8pZGH6SRpkNtoIAiw4";
    private static final String SALT = "DGv6ra1nlYgDCS1FRnbzlw";
    private static final String AUTH_SECRET = "BTBZMqHH6r4Tts7J_aSIgg";
    private static final String EXPECTED_BODY = "DGv6ra1nlYgDCS1FRnbzlwAAEABBBP4z9KsN6nGRTbVYI_c7VJSPQTBtkgcy27mlmlMoZIIgDll6e3vCYLocInmYWAmS6TlzAC8wEqKK6PBru3jl7A_yl95bQpu6cVPTpK4Mqgkf1CXztLVBSt2Ks3oZwbuwXPXLWyouBWLVWGNWQexSgSxsj_Qulcy4a-fN";

    @Test
    void encryptionMatchesRfc8291TestVector() throws Exception {
        KeyPair serverKeys = new KeyPair(WebPushCrypto.publicKey(b64d(AS_PUBLIC)), WebPushCrypto.privateKey(b64d(AS_PRIVATE)));

        byte[] body = WebPushCrypto.encrypt(PLAINTEXT.getBytes(StandardCharsets.UTF_8),
                b64d(UA_PUBLIC), b64d(AUTH_SECRET), b64d(SALT), serverKeys);

        assertEquals(EXPECTED_BODY, b64(body));
    }

    @Test
    void randomEncryptionsHaveFreshSaltAndKeyAndExpectedLength() throws Exception {
        byte[] plaintext = "{\"title\":\"x\"}".getBytes(StandardCharsets.UTF_8);
        byte[] a = WebPushCrypto.encrypt(plaintext, b64d(UA_PUBLIC), b64d(AUTH_SECRET));
        byte[] b = WebPushCrypto.encrypt(plaintext, b64d(UA_PUBLIC), b64d(AUTH_SECRET));
        // 86-byte header + plaintext + delimiter + 16-byte tag
        assertEquals(86 + plaintext.length + 1 + 16, a.length);
        assertNotEquals(b64(a), b64(b));
    }

    @Test
    void keyEncodingRoundTrips() throws Exception {
        KeyPair pair = WebPushCrypto.generateKeyPair();
        byte[] pub = WebPushCrypto.encodePublic((ECPublicKey) pair.getPublic());
        byte[] priv = WebPushCrypto.encodePrivate((ECPrivateKey) pair.getPrivate());
        assertEquals(65, pub.length);
        assertEquals(32, priv.length);
        assertEquals(pair.getPublic(), WebPushCrypto.publicKey(pub));
        assertEquals(((ECPrivateKey) pair.getPrivate()).getS(), WebPushCrypto.privateKey(priv).getS());
    }

    @Test
    void vapidHeaderIsAVerifiableEs256Jwt() throws Exception {
        KeyPair pair = WebPushCrypto.generateKeyPair();
        String publicKey = b64(WebPushCrypto.encodePublic((ECPublicKey) pair.getPublic()));

        String header = WebPushCrypto.vapidAuthorization("https://fcm.googleapis.com", "https://animelivecountdown.com",
                1_800_000_000L, (ECPrivateKey) pair.getPrivate(), publicKey);

        assertTrue(header.startsWith("vapid t="));
        assertTrue(header.endsWith(", k=" + publicKey));
        String jwt = header.substring("vapid t=".length(), header.indexOf(", k="));
        String[] parts = jwt.split("\\.");
        assertEquals(3, parts.length);
        assertEquals("{\"typ\":\"JWT\",\"alg\":\"ES256\"}", new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8));
        assertEquals("{\"aud\":\"https://fcm.googleapis.com\",\"exp\":1800000000,\"sub\":\"https://animelivecountdown.com\"}",
                new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8));

        byte[] signature = Base64.getUrlDecoder().decode(parts[2]);
        assertEquals(64, signature.length); // raw R||S, as JWS requires
        Signature verifier = Signature.getInstance("SHA256withECDSAinP1363Format");
        verifier.initVerify(pair.getPublic());
        verifier.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
        assertTrue(verifier.verify(signature));
    }
}
