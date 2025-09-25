package com.example.cinemaster.util;

import com.example.cinemaster.exception.AppException;
import com.example.cinemaster.exception.ErrorCode;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@Slf4j
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.access-minutes}")
    private Long expirationMinutes;

    private MACSigner signer;

    @PostConstruct
    public void init() throws JOSEException {
        if (secret.getBytes().length < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 256-bit (32 bytes) long");
        }
        this.signer = new MACSigner(secret.getBytes());
        log.info("JwtUtil initialized with secret length: {}", secret.length());
    }

    /** Tạo token JWT */
    public String generateToken(Integer accountId, String phone, String role) throws JOSEException {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMinutes * 60 * 1000);

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .claim("accountId", accountId)
                .claim("phone", phone)
                .claim("role", role)
                .issueTime(now)
                .expirationTime(expiration)
                .build();

        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        signedJWT.sign(signer);

        return signedJWT.serialize();
    }

    /** Validate token, trả về true/false, không crash nếu token hết hạn */
    public boolean validateToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);

            // Kiểm tra signature
            boolean validSignature = signedJWT.verify(new MACVerifier(secret.getBytes()));

            // Kiểm tra expiration
            Date expiration = signedJWT.getJWTClaimsSet().getExpirationTime();
            if (expiration.before(new Date())) {
                log.warn("Token đã hết hạn");
                return false; // token hết hạn
            }

            return validSignature;
        } catch (Exception e) {
            log.error("Token không hợp lệ: {}", e.getMessage());
            return false;
        }
    }

    /** Extract accountId */
    public Integer extractAccountId(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            return signedJWT.getJWTClaimsSet().getLongClaim("accountId").intValue();
        } catch (Exception e) {
            log.error("Không thể lấy accountId từ token: {}", e.getMessage());
            return null;
        }
    }

    /** Extract phone */
    public String extractPhone(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            return signedJWT.getJWTClaimsSet().getStringClaim("phone");
        } catch (Exception e) {
            log.error("Không thể lấy phone từ token: {}", e.getMessage());
            return null;
        }
    }

    /** Extract role */
    public String extractRole(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            return signedJWT.getJWTClaimsSet().getStringClaim("role");
        } catch (Exception e) {
            log.error("Không thể lấy role từ token: {}", e.getMessage());
            return null;
        }
    }
}
