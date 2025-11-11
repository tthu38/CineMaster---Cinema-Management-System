package com.example.cinemaster.controller;

import com.example.cinemaster.dto.request.LoginRequest;
import com.example.cinemaster.dto.response.AccountResponse;
import com.example.cinemaster.dto.response.ApiResponse;
import com.example.cinemaster.dto.response.AuthResponse;
import com.example.cinemaster.entity.Account;
import com.example.cinemaster.entity.Role;
import com.example.cinemaster.repository.AccountRepository;
import com.example.cinemaster.repository.RoleRepository;
import com.example.cinemaster.security.AccountPrincipal;
import com.example.cinemaster.service.AuthService;
import com.example.cinemaster.service.JwtService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;


@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;

    @Value("${google.oauth2.client-id}")
    private String googleClientId;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(
                    ApiResponse.<AuthResponse>builder()
                            .code(200)
                            .message("Đăng nhập thành công")
                            .result(response)
                            .build()
            );
        } catch (BadCredentialsException e) {
            log.warn("Login failed for phone: {}", request.getPhoneNumber());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    ApiResponse.<Void>builder()
                            .code(HttpStatus.UNAUTHORIZED.value())
                            .message(e.getMessage())
                            .build()
            );
        } catch (Exception e) {
            log.error("Unexpected error during login", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.<Void>builder()
                            .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .message("Đã có lỗi xảy ra, vui lòng thử lại sau")
                            .build()
            );
        }

    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> googleLogin(@RequestBody Map<String, String> body) {
        String idTokenStr = body.get("token");
        if (idTokenStr == null || idTokenStr.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), GsonFactory.getDefaultInstance()
            )
                    .setAudience(Collections.singletonList(googleClientId))
                    .setIssuer("https://accounts.google.com")
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenStr);
            if (idToken == null) {
                log.error("Google token verify FAIL");
                return ResponseEntity.status(401).build();
            }

            Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String name = (String) payload.get("name");

            log.info("Google login OK: email={}, name={}", email, name);

            Account account = accountRepository.findByEmailWithRole(email)
                    .orElseGet(() -> {
                        Account acc = new Account();
                        acc.setEmail(email);
                        acc.setFullName(name != null ? name : "Google User");
                        acc.setPassword("GOOGLE_LOGIN");
                        acc.setIsActive(true);
                        acc.setCreatedAt(LocalDate.now());

                        Role role = roleRepository.findByRoleName("Customer")
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy Role"));
                        acc.setRole(role);
                        return accountRepository.save(acc);
                    });

            String roleName = account.getRole() != null ? account.getRole().getRoleName() : "Customer";
            String token = jwtService.generateAccessToken(account.getAccountID(), account.getEmail(), roleName);

            AuthResponse response = AuthResponse.builder()
                    .accessToken(token)
                    .tokenType("Bearer")
                    .expiresIn(3600)
                    .email(account.getEmail())
                    .fullName(account.getFullName())
                    .role(roleName)
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Google login thất bại", e);
            return ResponseEntity.status(401).build();
        }
    }


    // ===== Logout =====
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid Authorization header"));
        }
        String token = authHeader.substring(7);
        jwtService.invalidateToken(token);
        log.info("Token invalidated: {}", token);
        return ResponseEntity.ok(Map.of("message", "Logout successful"));
    }

    @GetMapping("/me")
    public ResponseEntity<AccountResponse> getCurrentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AccountPrincipal user)) {
            return ResponseEntity.status(401).build();
        }

        AccountResponse res = AccountResponse.builder()
                .accountID(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .roleName(user.getRole())
                .branchId(user.getBranchId())
                .branchName(user.getBranchName())
                .build();

        return ResponseEntity.ok(res);
    }
}



