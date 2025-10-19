package com.example.cinemaster.controller;

import com.example.cinemaster.service.GoogleSheetsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final GoogleSheetsService sheetsService;
    // in-memory map để lưu amount & trạng thái (ko DB)
    private final ConcurrentHashMap<String, String> statusMap = new ConcurrentHashMap<>();

    public PaymentController(GoogleSheetsService sheetsService) {
        this.sheetsService = sheetsService;
    }

    // Tạo "order": trả về code + thông tin tài khoản để user chuyển khoản
    @PostMapping("/create")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> body) {
        long amount;
        try {
            amount = Long.parseLong(body.get("amount").toString());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error","invalid amount"));
        }
        String code = "CINE" + UUID.randomUUID().toString().replaceAll("-", "").substring(0,8).toUpperCase();
        // Lưu trạng thái pending
        statusMap.put(code, "pending");
        // Trả về: code, account info, hướng dẫn (khách ghi code vào nội dung chuyển khoản)
        return ResponseEntity.ok(Map.of(
                "code", code,
                "amount", amount,
                "accountNumber", "00004053275",
                "bankName", "TPBank",
                "noteHint", "Vui lòng ghi nội dung chuyển khoản: " + code,
                "pollUrl", "/api/v1/payments/status/" + code
        ));
    }

    // Frontend poll: kiểm tra sheet xem đã có giao dịch chưa
    @GetMapping("/status/{code}")
    public ResponseEntity<?> checkStatus(@PathVariable String code) {
        try {
            Map<String, String> r = sheetsService.findTransactionByCode(code);
            if ("true".equals(r.get("found"))) {
                statusMap.put(code, "paid"); // update in-memory
                return ResponseEntity.ok(Map.of("status","paid", "meta", r));
            } else {
                return ResponseEntity.ok(Map.of("status","pending"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
