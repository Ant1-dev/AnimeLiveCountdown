package com.Toine.animeCountdownBackend.anilist;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AniListOAuthStateTest {
    private static final String SECRET = Base64.getEncoder().encodeToString(new byte[32]);
    private static final String OTHER_SECRET = Base64.getEncoder().encodeToString("x".repeat(32).getBytes());
    private static final Instant T0 = Instant.parse("2026-11-11T12:00:00Z");

    private final AniListOAuthState state = new AniListOAuthState(SECRET, Clock.fixed(T0, ZoneOffset.UTC));

    @Test
    void acceptsMatchingStateAndCookie() {
        AniListOAuthState.Issued issued = state.issue(42L);

        assertEquals(Optional.of(42L), state.verify(issued.state(), issued.nonce()));
    }

    @Test
    void rejectsCookieFromAnotherFlow() {
        AniListOAuthState.Issued victim = state.issue(1L);
        AniListOAuthState.Issued attacker = state.issue(2L);

        // Attacker's state completed in the victim's browser (victim's cookie) must not link anything
        assertTrue(state.verify(attacker.state(), victim.nonce()).isEmpty());
        assertTrue(state.verify(attacker.state(), null).isEmpty());
    }

    @Test
    void rejectsExpiredState() {
        AniListOAuthState.Issued issued = state.issue(42L);
        AniListOAuthState later = new AniListOAuthState(SECRET,
                Clock.fixed(T0.plus(AniListOAuthState.LIFETIME).plusSeconds(1), ZoneOffset.UTC));

        assertTrue(later.verify(issued.state(), issued.nonce()).isEmpty());
    }

    @Test
    void rejectsStateSignedWithAnotherKeyOrTampered() {
        AniListOAuthState other = new AniListOAuthState(OTHER_SECRET, Clock.fixed(T0, ZoneOffset.UTC));
        AniListOAuthState.Issued forged = other.issue(42L);

        assertTrue(state.verify(forged.state(), forged.nonce()).isEmpty());
        AniListOAuthState.Issued issued = state.issue(42L);
        assertTrue(state.verify(issued.state() + "x", issued.nonce()).isEmpty());
        assertTrue(state.verify("not-a-jwt", issued.nonce()).isEmpty());
    }
}
