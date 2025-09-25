package com.example.cinemaster.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.access-minutes}")
    private long accessMinutes;

    // Blacklist token đã logout
    private final Set<String> invalidatedTokens = ConcurrentHashMap.newKeySet();

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String generateAccessToken(Integer accountId, String phone, String role) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(accessMinutes * 60);

        return Jwts.builder()
                .setSubject(phone)
                .claim("accountId", accountId)
                .claim("role", role)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiry))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            if (invalidatedTokens.contains(token)) return false;
            Jwts.parser()
                    .verifyWith((SecretKey) getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public void invalidateToken(String token) {
        invalidatedTokens.add(token);
    }

    public String extractPhone(String token) {
        return Jwts.parser()
                .verifyWith((SecretKey) getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }
}
