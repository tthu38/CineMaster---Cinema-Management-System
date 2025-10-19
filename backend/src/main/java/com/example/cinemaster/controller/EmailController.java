//package com.example.cinemaster.controller;
//
//import com.example.cinemaster.service.EmailService;
//import jakarta.mail.MessagingException;
//import lombok.RequiredArgsConstructor;
//import org.springframework.web.bind.annotation.*;
//import java.math.BigDecimal;
//import java.time.LocalDateTime;
//
//@RestController
//@RequestMapping("/api/v1/email")
//@RequiredArgsConstructor
//public class EmailController {
//
//    private final EmailService emailService;
//
//    @GetMapping("/test-mail")
//    public String testMail() {
//        try {
//            emailService.sendBookingConfirmationEmail(
//                    "ohtthu0308@gmail.com",
//                    "CM-20251018-1234",
//                    "Inside Out 2",
//                    "Phòng chiếu 4 - Rạp Quận 7",
//                    "G05, G06",
//                    LocalDateTime.now(),
//                    new BigDecimal("45000"),
//                    new BigDecimal("180000"),
//                    new BigDecimal("30000"),
//                    new BigDecimal("150000"),
//                    "Tầng 3, SC VivoCity, Nguyễn Văn Linh, Q.7, TP.HCM",
//                    "https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=https://cinemaster.vn/ticket/12345"
//            );
//            System.out.println("✅ Mail đã gửi xong!");
//            return "✅ Email thử đã được gửi!";
//        } catch (Exception ex) {
//            ex.printStackTrace();
//            return "❌ Gửi mail thất bại: " + ex.getMessage();
//        }
//    }
//
//}
