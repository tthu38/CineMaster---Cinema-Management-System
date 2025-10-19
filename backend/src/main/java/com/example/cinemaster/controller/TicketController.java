package com.example.cinemaster.controller;

import com.example.cinemaster.dto.request.ComboRequest;
import com.example.cinemaster.dto.request.TicketComboRequest;
import com.example.cinemaster.dto.request.TicketCreateRequest;
import com.example.cinemaster.dto.response.ApiResponse;
import com.example.cinemaster.dto.response.TicketDiscountResponse;
import com.example.cinemaster.dto.response.TicketResponse;
import com.example.cinemaster.entity.Ticket;
import com.example.cinemaster.mapper.TicketMapper;
import com.example.cinemaster.repository.TicketRepository;
import com.example.cinemaster.service.DiscountService;
import com.example.cinemaster.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketRepository ticketRepository;
    private final TicketService ticketService;
    private final DiscountService discountService;
    private final TicketMapper ticketMapper;

    @PostMapping
    public ResponseEntity<TicketResponse> createOrUpdateTicket(@RequestBody TicketCreateRequest req) {
        return ResponseEntity.ok(ticketService.createOrUpdateTicket(req));
    }

    @GetMapping("/{ticketId}")
    public ResponseEntity<TicketResponse> getTicket(@PathVariable Integer ticketId) {
        return ResponseEntity.ok(ticketService.getTicket(ticketId));
    }

    @GetMapping("/{ticketId}/seats")
    public ResponseEntity<List<Integer>> getHeldSeats(@PathVariable Integer ticketId) {
        return ResponseEntity.ok(ticketService.getHeldSeats(ticketId));
    }

    @PutMapping("/{ticketId}/seats")
    public ResponseEntity<TicketResponse> replaceSeats(
            @PathVariable Integer ticketId,
            @RequestBody List<Integer> seatIds) {
        return ResponseEntity.ok(ticketService.replaceSeats(ticketId, seatIds));
    }

//    @PostMapping("/{ticketId}/confirm")
//    public ResponseEntity<String> confirmPayment(@PathVariable Integer ticketId) {
//        ticketService.confirmPayment(ticketId);
//        return ResponseEntity.ok("Thanh toán thành công!");
//    }

    @PostMapping("/{ticketId}/cancel")
    public ResponseEntity<String> cancelTicket(@PathVariable Integer ticketId) {
        ticketService.cancelTicket(ticketId);
        return ResponseEntity.ok("Hủy vé thành công!");
    }

    @GetMapping("/occupied/{showtimeId}")
    public ResponseEntity<List<Integer>> getOccupiedSeats(@PathVariable Integer showtimeId) {
        List<Integer> occupiedSeatIds = ticketRepository.findOccupiedSeatIdsByShowtime(showtimeId);
        return ResponseEntity.ok(occupiedSeatIds);
    }

    @PreAuthorize("hasAnyRole('Customer','Staff','Manager','Admin')")
    @PostMapping("/{ticketId}/apply-discount/{code}")
    public ResponseEntity<ApiResponse<TicketDiscountResponse>> applyDiscount(
            @PathVariable Integer ticketId,
            @PathVariable String code) {

        TicketDiscountResponse result = discountService.applyDiscount(ticketId, code);

        ApiResponse<TicketDiscountResponse> api = ApiResponse.<TicketDiscountResponse>builder()
                .message("Discount applied successfully")
                .result(result)
                .build();

        return ResponseEntity.ok(api);
    }


    @PostMapping("/{ticketId}/confirm")
    public ResponseEntity<ApiResponse<String>> confirmPayment(
            @PathVariable Integer ticketId,
            @RequestBody(required = false) Map<String, Object> body) {

        // 📨 1️⃣ Lấy email tùy chọn
        String customEmail = null;
        if (body != null && body.get("email") != null) {
            customEmail = body.get("email").toString();
        }

        // 🍿 2️⃣ Lấy danh sách combo (nếu có)
        List<TicketComboRequest> combos = null;
        if (body != null && body.get("combos") instanceof List<?>) {
            combos = ((List<?>) body.get("combos")).stream()
                    .filter(Map.class::isInstance)
                    .map(item -> {
                        Map<?, ?> map = (Map<?, ?>) item;
                        Integer comboId = (Integer) map.get("comboId");
                        Integer quantity = (Integer) map.get("quantity");
                        return new TicketComboRequest(comboId, quantity);
                    })
                    .toList();
        }

        // 💳 3️⃣ Xác nhận thanh toán + gửi mail
        ticketService.confirmPayment(ticketId, combos, customEmail);

        // ✅ 4️⃣ Phản hồi API chuẩn
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .message("Thanh toán thành công — Vé đã được xác nhận và gửi email cho khách hàng.")
                .build());
    }


    //    @PreAuthorize("hasAnyRole('CUSTOMER','STAFF','MANAGER','ADMIN')")
    @PostMapping("/{ticketId}/add-combos")
    public ResponseEntity<?> addCombosToTicket(
            @PathVariable Integer ticketId,
            @RequestBody List<TicketComboRequest> combos) {
        ticketService.addCombosToTicket(ticketId, combos);
        return ResponseEntity.ok(Map.of("message", "Đã thêm combo vào vé"));
    }


}
