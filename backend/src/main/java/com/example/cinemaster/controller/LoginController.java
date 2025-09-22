package com.example.cinemaster.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class LoginController {

    // API test login status
    @GetMapping("/me")
    public Map<String, Object> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> response = new HashMap<>();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            response.put("status", "authenticated");
            response.put("username", auth.getName());
        } else {
            response.put("status", "unauthenticated");
        }
        return response;
    }

    // API login thành công đã được Spring Security xử lý,
    // bạn chỉ cần custom success/failure handler trong SecurityConfig.
    // FE sẽ fetch POST /demo/login (Spring Security xử lý),
    // còn AuthController chỉ để expose thông tin user sau khi login.

    @PostMapping("/logout")
    public Map<String, String> logout(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request.logout(); // Spring Security logout
        Map<String, String> result = new HashMap<>();
        result.put("status", "logged_out");
        return result;
    }
}
