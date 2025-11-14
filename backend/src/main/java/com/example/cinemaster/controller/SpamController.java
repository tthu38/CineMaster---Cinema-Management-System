package com.example.cinemaster.controller;

import com.example.cinemaster.service.AIService;
import com.example.cinemaster.service.ContactRequestService;
import com.example.cinemaster.service.MovieFeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/spam")
@RequiredArgsConstructor
public class SpamController {

    private final AIService aiService;
    private final ContactRequestService contactService;
    private final MovieFeedbackService feedbackService;

    /* ========================= CHECK CONTACT ========================= */
    @PostMapping("/contact/{id}")
    public ResponseEntity<?> scanContact(@PathVariable Integer id) {
        var contact = contactService.getById(id);
        boolean spam = aiService.isSpam(contact.getMessage());

        return ResponseEntity.ok(spam);
    }

    /* ========================= CHECK FEEDBACK ========================= */
    @PostMapping("/feedback/{id}")
    public ResponseEntity<?> scanFeedback(@PathVariable Integer id) {
        var fb = feedbackService.getById(id);
        boolean spam = aiService.isSpam(fb.getComment());

        return ResponseEntity.ok(spam);
    }

    /* ========================= ADMIN BAN USER ========================= */
    @PreAuthorize("hasRole('Admin')")
    @PutMapping("/ban/{id}")
    public ResponseEntity<?> banUser(@PathVariable Integer id) {
        feedbackService.banUser(id); // Bạn sẽ tạo hàm này
        return ResponseEntity.ok("User banned!");
    }
}

