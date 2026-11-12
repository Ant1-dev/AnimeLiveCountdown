package com.Toine.animeCountdownBackend.anilist;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AniListTokenCipherTest {

    @Test
    void roundTripsAndDoesNotStorePlaintext() {
        AniListTokenCipher cipher = new AniListTokenCipher("secret-a");
        String encrypted = cipher.encrypt("anilist-access-token");

        assertFalse(encrypted.contains("anilist-access-token"));
        assertNotEquals(encrypted, cipher.encrypt("anilist-access-token"), "fresh IV per encryption");
        assertEquals("anilist-access-token", cipher.decrypt(encrypted));
    }

    @Test
    void rejectsTokensEncryptedWithAnotherKeyOrTampered() {
        String encrypted = new AniListTokenCipher("secret-a").encrypt("token");

        assertThrows(IllegalArgumentException.class, () -> new AniListTokenCipher("secret-b").decrypt(encrypted));
        assertThrows(IllegalArgumentException.class,
                () -> new AniListTokenCipher("secret-a").decrypt(encrypted.substring(0, encrypted.length() - 4) + "AAAA"));
        assertThrows(IllegalArgumentException.class, () -> new AniListTokenCipher("secret-a").decrypt("%%%"));
    }
}
