package com.example.cinemaster.controller;

import com.example.cinemaster.dto.request.ComboRequest;
import com.example.cinemaster.dto.request.TicketComboRequest;
import com.example.cinemaster.dto.request.TicketCreateRequest;
import com.example.cinemaster.dto.response.*;
import com.example.cinemaster.entity.Account;
import com.example.cinemaster.entity.Ticket;
import com.example.cinemaster.mapper.TicketMapper;
import com.example.cinemaster.repository.TicketRepository;
import com.example.cinemaster.service.DiscountService;
import com.example.cinemaster.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    /* =============================================================
       🔹 BASIC TICKET OPERATIONS
    ============================================================= */
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

    /* =============================================================
       🔹 DISCOUNT & PAYMENT
    ============================================================= */
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

        String customEmail = null;
        if (body != null && body.get("email") != null) {
            customEmail = body.get("email").toString();
        }

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

        ticketService.confirmPayment(ticketId, combos, customEmail);

        return ResponseEntity.ok(ApiResponse.<String>builder()
                .message("Thanh toán thành công — Vé đã được xác nhận và gửi email cho khách hàng.")
                .build());
    }

    @PostMapping("/{ticketId}/add-combos")
    public ResponseEntity<?> addCombosToTicket(
            @PathVariable Integer ticketId,
            @RequestBody List<TicketComboRequest> combos) {
        ticketService.addCombosToTicket(ticketId, combos);
        return ResponseEntity.ok(Map.of("message", "Đã thêm combo vào vé"));
    }

    /* =============================================================
       🔹 CUSTOMER / STAFF / MANAGER / ADMIN TICKET MANAGEMENT
    ============================================================= */

    // 🔸 CUSTOMER gửi yêu cầu hủy vé
    @PutMapping("/{ticketId}/cancel-request")
    @PreAuthorize("hasRole('Customer')")
    public ResponseEntity<ApiResponse<TicketResponse>> requestCancel(
            @PathVariable Integer ticketId,
            @AuthenticationPrincipal Account account) {

        return ResponseEntity.ok(ApiResponse.<TicketResponse>builder()
                .code(1000)
                .message("Đã gửi yêu cầu hủy vé")
                .result(ticketService.requestCancel(ticketId, account))
                .build());
    }

    // 🔸 STAFF duyệt hủy vé
    @PutMapping("/{ticketId}/approve-cancel")
    @PreAuthorize("hasAnyRole('Staff','Manager','Admin')")
    public ResponseEntity<ApiResponse<TicketResponse>> approveCancel(
            @PathVariable Integer ticketId,
            @RequestParam Integer accountId) {

        Account staff = new Account();
        staff.setAccountID(accountId);

        return ResponseEntity.ok(ApiResponse.<TicketResponse>builder()
                .code(1000)
                .message("Đã duyệt hủy vé")
                .result(ticketService.approveCancel(ticketId, staff))
                .build());
    }

    // 🔸 STAFF duyệt hoàn tiền
    @PutMapping("/{ticketId}/approve-refund")
    @PreAuthorize("hasAnyRole('Staff','Manager','Admin')")
    public ResponseEntity<ApiResponse<TicketResponse>> approveRefund(
            @PathVariable Integer ticketId,
            @RequestParam Integer accountId) {

        Account staff = new Account();
        staff.setAccountID(accountId);

        return ResponseEntity.ok(ApiResponse.<TicketResponse>builder()
                .code(1000)
                .message("Đã hoàn tiền vé")
                .result(ticketService.approveRefund(ticketId, staff))
                .build());
    }

    // 🔸 STAFF xem danh sách vé chờ hủy theo chi nhánh
    @GetMapping("/branch/{branchId}/pending-cancel")
    @PreAuthorize("hasRole('Staff')")
    public ResponseEntity<ApiResponse<List<TicketResponse>>> getPendingCancelTickets(
            @PathVariable Integer branchId) {

        return ResponseEntity.ok(ApiResponse.<List<TicketResponse>>builder()
                .code(1000)
                .message("Danh sách vé chờ hủy")
                .result(ticketService.getPendingCancelTickets(branchId))
                .build());
    }

    // 🔸 CUSTOMER xem vé theo tài khoản
    @GetMapping("/account/{accountID}")
    @PreAuthorize("hasRole('Customer')")
    public ResponseEntity<List<TicketResponse>> getTicketsByAccount(@PathVariable Integer accountID) {
        return ResponseEntity.ok(ticketService.getTicketsByAccount(accountID));
    }

    // 🔸 STAFF xem vé theo chi nhánh
    @GetMapping("/branch/{branchId}")
    @PreAuthorize("hasRole('Staff')")
    public ResponseEntity<ApiResponse<List<TicketResponse>>> getTicketsByBranch(
            @PathVariable Integer branchId) {

        return ResponseEntity.ok(ApiResponse.<List<TicketResponse>>builder()
                .code(1000)
                .message("Danh sách vé theo chi nhánh")
                .result(ticketService.getTicketsByBranch(branchId))
                .build());
    }

    // 🔸 STAFF cập nhật trạng thái thủ công
    @PutMapping("/update-status/{ticketId}")
    @PreAuthorize("hasAnyRole('Staff','Manager','Admin')")
    public ResponseEntity<ApiResponse<TicketResponse>> updateTicketStatus(
            @PathVariable Integer ticketId,
            @RequestParam String status,
            @RequestParam Integer accountId) {

        Account staff = new Account();
        staff.setAccountID(accountId);

        return ResponseEntity.ok(ApiResponse.<TicketResponse>builder()
                .code(1000)
                .message("Cập nhật trạng thái thành công")
                .result(ticketService.updateTicketStatus(ticketId, status, staff))
                .build());
    }

    // 🔸 Chi tiết vé (dạng chi tiết mở rộng)
    @GetMapping("/detail/{id}")
    public ResponseEntity<ApiResponse<TicketDetailResponse>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.<TicketDetailResponse>builder()
                .code(1000)
                .message("Success")
                .result(ticketService.getById(id))
                .build());
    }

    // 🟢 Kiểm tra thanh toán online bằng Google Sheets
    @GetMapping("/{ticketId}/verify-payment")
    public ResponseEntity<?> verifyOnlinePayment(@PathVariable Integer ticketId) {
        try {
            TicketResponse res = ticketService.verifyOnlinePayment(ticketId);
            return ResponseEntity.ok(ApiResponse.success(res));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

}
