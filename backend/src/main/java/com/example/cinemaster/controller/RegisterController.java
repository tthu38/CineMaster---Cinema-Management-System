package com.example.cinemaster.controller;

import com.example.cinemaster.dto.request.RegisterRequest;
import com.example.cinemaster.service.RegisterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class RegisterController {

    @Autowired
    private RegisterService accountService;

    @PostMapping
    public String register(@RequestBody RegisterRequest request) {
        return accountService.register(request);
    }

    @GetMapping("/verify")
    public String verify(@RequestParam String email, @RequestParam String code) {
        return accountService.verifyAccount(email, code);
    }
}


