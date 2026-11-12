package com.Toine.animeCountdownBackend.anilist;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

/**
 * The OAuth "state" for the AniList connect flow.
 *
 * The state is a short-lived signed token naming the app user who started the flow plus a random
 * nonce. The same nonce goes in an HttpOnly cookie on this browser. The callback accepts the code only
 * if the signature is valid, the token hasn't expired, and the cookie matches, so a crafted AniList
 * link can't attach someone else's AniList account to your user (or yours to theirs).
 */
@Component
public class AniListOAuthState {
    static final Duration LIFETIME = Duration.ofMinutes(10);
    private static final String PURPOSE = "anilist-link";

    private final Key key;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    @org.springframework.beans.factory.annotation.Autowired
    public AniListOAuthState(@Value("${app.jwt.secret}") String jwtSecret) {
        this(jwtSecret, Clock.systemUTC());
    }

    AniListOAuthState(String jwtSecret, Clock clock) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
        this.clock = clock;
    }

    public record Issued(String state, String nonce) {
    }

    public Issued issue(long userId) {
        byte[] nonceBytes = new byte[24];
        random.nextBytes(nonceBytes);
        String nonce = Base64.getUrlEncoder().withoutPadding().encodeToString(nonceBytes);
        Date now = Date.from(clock.instant());
        String state = Jwts.builder()
                .setSubject(Long.toString(userId))
                .claim("purpose", PURPOSE)
                .claim("nonce", nonce)
                .setIssuedAt(now)
                .setExpiration(Date.from(clock.instant().plus(LIFETIME)))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
        return new Issued(state, nonce);
    }

    /** Returns the user ID the flow was started for, or empty if the state or cookie doesn't check out. */
    public Optional<Long> verify(String state, String cookieNonce) {
        if (state == null || state.isBlank() || cookieNonce == null || cookieNonce.isBlank()) {
            return Optional.empty();
        }
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .setClock(() -> Date.from(clock.instant()))
                    .build()
                    .parseClaimsJws(state)
                    .getBody();
            String nonce = claims.get("nonce", String.class);
            if (!PURPOSE.equals(claims.get("purpose", String.class)) || nonce == null
                    || !MessageDigest.isEqual(nonce.getBytes(StandardCharsets.UTF_8),
                                              cookieNonce.getBytes(StandardCharsets.UTF_8))) {
                return Optional.empty();
            }
            return Optional.of(Long.parseLong(claims.getSubject()));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
