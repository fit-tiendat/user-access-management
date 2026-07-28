package com.r2s.core.security;


import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;


@Component
public class JwtService {
    private static final int MINIMUM_HS256_KEY_BYTES = 32;

    private final Key signingKey;
    private final List<Key> verificationKeys;
    private final long expirationMillis;

    public JwtService(@Value("${security.jwt.secret}") String secret,
                      @Value("${security.jwt.previous-secret:}") String previousSecret,
                      @Value("${security.jwt.expiration-minutes}") long expirationMinutes) {
        this.signingKey = decodeKey("security.jwt.secret", secret);
        this.verificationKeys = new ArrayList<>();
        this.verificationKeys.add(signingKey);

        if (previousSecret != null && !previousSecret.isBlank()) {
            this.verificationKeys.add(decodeKey("security.jwt.previous-secret", previousSecret));
        }

        if (expirationMinutes <= 0) {
            throw new IllegalArgumentException("security.jwt.expiration-minutes must be greater than zero");
        }
        this.expirationMillis = Math.multiplyExact(expirationMinutes, 60_000L);
    }

    public String generateToken(String username, Map<String, Object> claims) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(username)
                .addClaims(claims)
                .setIssuedAt(Date.from(now))
                .setExpiration(new Date(now.toEpochMilli() + expirationMillis))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return parse(token).getBody().getSubject();
    }


    public boolean isValid(String token, String username) {
        try {
            return extractUsername(token).equals(username) && !isExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private boolean isExpired(String token) {
        Date exp = parse(token).getBody().getExpiration();
        return exp.before(new Date());
    }

    private Jws<Claims> parse(String token) {
        JwtException lastFailure = null;

        for (Key verificationKey : verificationKeys) {
            try {
                return Jwts.parserBuilder()
                        .setSigningKey(verificationKey)
                        .build()
                        .parseClaimsJws(token);
            } catch (JwtException failure) {
                lastFailure = failure;
            }
        }

        throw lastFailure == null
                ? new JwtException("JWT verification failed")
                : lastFailure;
    }

    private static Key decodeKey(String propertyName, String encodedSecret) {
        if (encodedSecret == null || encodedSecret.isBlank()) {
            throw new IllegalArgumentException(propertyName + " must be configured");
        }

        final byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(encodedSecret);
        } catch (RuntimeException failure) {
            throw new IllegalArgumentException(propertyName + " must be valid Base64", failure);
        }

        if (keyBytes.length < MINIMUM_HS256_KEY_BYTES) {
            throw new IllegalArgumentException(propertyName + " must contain at least 256 bits");
        }

        return Keys.hmacShaKeyFor(keyBytes);
    }
    public String extractRole(String token) {
        Object role = parse(token).getBody().get("role");
        return role == null ? null : role.toString();
    }
    // core/src/main/java/com/r2s/core/security/JwtService.java
    public boolean isSignatureAndExpiryValid(String token) {
        try {
            // parse(token) đã kiểm tra chữ ký + hạn
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

}
