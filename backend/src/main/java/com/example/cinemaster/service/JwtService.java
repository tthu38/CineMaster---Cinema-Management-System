package com.example.cinemaster.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
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

    // Lấy secret key
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    // Sinh access token
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

    // Parse và lấy toàn bộ claims
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // Validate token (có check blacklist)
    public boolean validateToken(String token) {
        try {
            if (invalidatedTokens.contains(token)) return false;
            extractAllClaims(token); // Nếu parse thành công thì token hợp lệ
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // Invalidate token khi logout
    public void invalidateToken(String token) {
        invalidatedTokens.add(token);
    }

    // Extract phone (subject)
    public String extractPhone(String token) {
        return extractAllClaims(token).getSubject();
    }

    // Extract accountId
    public Integer extractAccountId(String token) {
        return extractAllClaims(token).get("accountId", Integer.class);
    }

    // Extract role
    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }
}
