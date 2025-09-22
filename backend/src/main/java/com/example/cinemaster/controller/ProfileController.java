package com.example.cinemaster.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/demo")
public class ProfileController {

    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> profile(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        if (authentication != null && authentication.isAuthenticated()) {
            response.put("status", "authenticated");
            response.put("username", authentication.getName());
            return ResponseEntity.ok(response);
        } else {
            response.put("status", "unauthenticated");
            return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED).body(response);
        }
    }
}


