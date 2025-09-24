package com.example.cinemaster.controller;

import com.example.cinemaster.dto.request.UpdateProfileRequest;
import com.example.cinemaster.dto.response.ProfileResponse;
import com.example.cinemaster.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/profile")
    public ResponseEntity<ProfileResponse> getProfile() {
        return ResponseEntity.ok(profileService.getProfile());
    }

    @PutMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProfileResponse> updateProfile(
            @ModelAttribute UpdateProfileRequest req,
            @RequestPart(value = "avatarFile", required = false) MultipartFile avatarFile
    ) {
        return ResponseEntity.ok(profileService.updateProfile(req, avatarFile));
    }


    @DeleteMapping("/profile")
    public ResponseEntity<?> deleteProfile() {
        profileService.deleteProfile();
        return ResponseEntity.ok("Profile deleted successfully");
    }
}

