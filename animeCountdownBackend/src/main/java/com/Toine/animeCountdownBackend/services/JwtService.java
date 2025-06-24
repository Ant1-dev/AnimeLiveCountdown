package com.Toine.animeCountdownBackend.services;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {
    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration}")
    private long jwtExpiration;

    @Value("${app.jwt.refresh-expiration}")
    private long refreshExpiration;

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims,T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return buildToken(extraClaims, userDetails, jwtExpiration, "access");
    }

    public long getExpirationTime() {
        return jwtExpiration;
    }

    public String generateRefreshToken(UserDetails userDetails) {
        Map<String,Object> claims = new HashMap<>();
        return buildToken(claims, userDetails, refreshExpiration, "refresh");
    }

    public long getRefreshExpirationTime() {
        return refreshExpiration;
    }

    private String buildToken(Map<String, Object> extraClaims,
                              UserDetails userDetails,
                              long expiration,
                              String tokenType
    ) {
        extraClaims.put("tokenType", tokenType);
        return Jwts
                .builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isRefreshTokenValid(String token, UserDetails userDetails) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        try {
            final String username = extractUsername(token);
            final String tokenType = extractClaim(token, claims -> claims.get("tokenType", String.class));
            return (username.equals(userDetails.getUsername()))
                    && !isTokenExpired(token)
                    && "refresh".equals(tokenType);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        try {
            final String username = extractUsername(token);
            return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts
                .parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

}
