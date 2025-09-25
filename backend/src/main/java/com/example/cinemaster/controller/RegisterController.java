package com.example.cinemaster.controller;

import com.example.cinemaster.dto.request.RegisterRequest;
import com.example.cinemaster.service.RegisterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", allowCredentials = "false")
public class RegisterController {

    private final RegisterService accountService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request,
                                      BindingResult result) {
        // Validate theo @Pattern, @Size trong DTO
        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(
                    result.getAllErrors().stream()
                            .map(err -> err.getDefaultMessage())
                            .toList()
            );
        }

        String serviceResult = accountService.register(request);

        // 👉 Check lỗi trùng email
        if (serviceResult.contains("Email đã tồn tại")) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(serviceResult);
        }

        // 👉 Check lỗi trùng số điện thoại
        if (serviceResult.contains("Số điện thoại đã tồn tại")) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(serviceResult);
        }

        // 👉 Check lỗi không tìm thấy role
        if (serviceResult.contains("Không tìm thấy Role")) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(serviceResult);
        }

        // 👉 Mặc định: đăng ký thành công
        return ResponseEntity.ok(serviceResult);
    }

    @GetMapping("/verify")
    public ResponseEntity<String> verify(@RequestParam String email,
                                         @RequestParam String code) {
        return ResponseEntity.ok(accountService.verifyAccount(email, code));
    }
}
