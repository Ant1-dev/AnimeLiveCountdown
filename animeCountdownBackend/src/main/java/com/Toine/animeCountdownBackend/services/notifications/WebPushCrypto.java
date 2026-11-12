package com.Toine.animeCountdownBackend.services.notifications;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.*;
import java.util.Arrays;
import java.util.Base64;

/**
 * Web Push crypto with the JDK only: payload encryption (RFC 8291, "aes128gcm" content coding from
 * RFC 8188) and the VAPID JWT (RFC 8292, ES256). Keys use the usual Web Push encodings: public keys
 * are uncompressed P-256 points (65 bytes) and private keys are 32-byte scalars, both base64url.
 */
public final class WebPushCrypto {

    private static final int RECORD_SIZE = 4096;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final ECParameterSpec P256 = p256();

    private WebPushCrypto() {
    }

    // ---- Encryption (RFC 8291) ----

    /** Encrypts {@code plaintext} for a browser subscription with a fresh salt and server key. */
    public static byte[] encrypt(byte[] plaintext, byte[] uaPublic, byte[] authSecret) throws GeneralSecurityException {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return encrypt(plaintext, uaPublic, authSecret, salt, generateKeyPair());
    }

    static byte[] encrypt(byte[] plaintext, byte[] uaPublicBytes, byte[] authSecret, byte[] salt, KeyPair serverKeys)
            throws GeneralSecurityException {
        if (plaintext.length > RECORD_SIZE - 17 - 86) {
            throw new IllegalArgumentException("Push payload too large");
        }
        ECPublicKey uaPublic = publicKey(uaPublicBytes);
        byte[] asPublic = encodePublic((ECPublicKey) serverKeys.getPublic());

        KeyAgreement agreement = KeyAgreement.getInstance("ECDH");
        agreement.init(serverKeys.getPrivate());
        agreement.doPhase(uaPublic, true);
        byte[] ecdhSecret = agreement.generateSecret();

        byte[] prkKey = hmac(authSecret, ecdhSecret);
        byte[] keyInfo = concat("WebPush: info\0".getBytes(StandardCharsets.US_ASCII), uaPublicBytes, asPublic);
        byte[] ikm = hmac(prkKey, concat(keyInfo, new byte[]{1}));

        byte[] prk = hmac(salt, ikm);
        byte[] cek = Arrays.copyOf(hmac(prk, concat("Content-Encoding: aes128gcm\0".getBytes(StandardCharsets.US_ASCII), new byte[]{1})), 16);
        byte[] nonce = Arrays.copyOf(hmac(prk, concat("Content-Encoding: nonce\0".getBytes(StandardCharsets.US_ASCII), new byte[]{1})), 12);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(cek, "AES"), new GCMParameterSpec(128, nonce));
        // Single record: plaintext followed by the 0x02 last-record delimiter, no padding
        byte[] ciphertext = cipher.doFinal(concat(plaintext, new byte[]{2}));

        ByteBuffer header = ByteBuffer.allocate(16 + 4 + 1 + asPublic.length);
        header.put(salt).putInt(RECORD_SIZE).put((byte) asPublic.length).put(asPublic);
        return concat(header.array(), ciphertext);
    }

    // ---- VAPID (RFC 8292) ----

    /** Builds the value of the Authorization header for a push request to {@code audience}. */
    public static String vapidAuthorization(String audience, String subject, long expiresAtEpochSeconds,
                                            ECPrivateKey privateKey, String publicKeyBase64Url)
            throws GeneralSecurityException {
        String header = b64("{\"typ\":\"JWT\",\"alg\":\"ES256\"}".getBytes(StandardCharsets.UTF_8));
        String claims = b64(("{\"aud\":\"" + jsonEscape(audience) + "\",\"exp\":" + expiresAtEpochSeconds
                + ",\"sub\":\"" + jsonEscape(subject) + "\"}").getBytes(StandardCharsets.UTF_8));
        String signingInput = header + "." + claims;
        Signature signature = Signature.getInstance("SHA256withECDSAinP1363Format");
        signature.initSign(privateKey);
        signature.update(signingInput.getBytes(StandardCharsets.US_ASCII));
        String jwt = signingInput + "." + b64(signature.sign());
        return "vapid t=" + jwt + ", k=" + publicKeyBase64Url;
    }

    // ---- Key helpers ----

    public static KeyPair generateKeyPair() throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"), RANDOM);
        return generator.generateKeyPair();
    }

    public static ECPublicKey publicKey(byte[] uncompressed) throws GeneralSecurityException {
        if (uncompressed.length != 65 || uncompressed[0] != 0x04) {
            throw new InvalidKeyException("Expected an uncompressed P-256 point");
        }
        BigInteger x = new BigInteger(1, Arrays.copyOfRange(uncompressed, 1, 33));
        BigInteger y = new BigInteger(1, Arrays.copyOfRange(uncompressed, 33, 65));
        return (ECPublicKey) KeyFactory.getInstance("EC").generatePublic(new ECPublicKeySpec(new ECPoint(x, y), P256));
    }

    public static ECPrivateKey privateKey(byte[] scalar) throws GeneralSecurityException {
        if (scalar.length != 32) {
            throw new InvalidKeyException("Expected a 32-byte P-256 private key");
        }
        return (ECPrivateKey) KeyFactory.getInstance("EC").generatePrivate(new ECPrivateKeySpec(new BigInteger(1, scalar), P256));
    }

    public static byte[] encodePublic(ECPublicKey key) {
        byte[] out = new byte[65];
        out[0] = 0x04;
        copyUnsigned(key.getW().getAffineX(), out, 1);
        copyUnsigned(key.getW().getAffineY(), out, 33);
        return out;
    }

    public static byte[] encodePrivate(ECPrivateKey key) {
        byte[] out = new byte[32];
        copyUnsigned(key.getS(), out, 0);
        return out;
    }

    public static byte[] b64d(String base64Url) {
        String s = base64Url.trim().replace('+', '-').replace('/', '_');
        while (s.endsWith("=")) {
            s = s.substring(0, s.length() - 1);
        }
        return Base64.getUrlDecoder().decode(s);
    }

    public static String b64(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Prints a new VAPID key pair: {@code java WebPushCrypto.java} or run from the IDE. */
    public static void main(String[] args) throws GeneralSecurityException {
        KeyPair pair = generateKeyPair();
        System.out.println("VAPID_PUBLIC_KEY=" + b64(encodePublic((ECPublicKey) pair.getPublic())));
        System.out.println("VAPID_PRIVATE_KEY=" + b64(encodePrivate((ECPrivateKey) pair.getPrivate())));
    }

    private static void copyUnsigned(BigInteger value, byte[] out, int offset) {
        byte[] bytes = value.toByteArray();
        int start = Math.max(0, bytes.length - 32);
        int length = bytes.length - start;
        System.arraycopy(bytes, start, out, offset + 32 - length, length);
    }

    private static byte[] hmac(byte[] key, byte[] data) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data);
    }

    private static byte[] concat(byte[]... parts) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] part : parts) {
            out.writeBytes(part);
        }
        return out.toByteArray();
    }

    private static String jsonEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static ECParameterSpec p256() {
        try {
            AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
            parameters.init(new ECGenParameterSpec("secp256r1"));
            return parameters.getParameterSpec(ECParameterSpec.class);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("P-256 is not available", e);
        }
    }
}
