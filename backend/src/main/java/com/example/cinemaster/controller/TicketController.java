package com.example.cinemaster.controller;

import com.example.cinemaster.dto.response.TicketDetailResponse;
import com.example.cinemaster.dto.response.TicketResponse;
import com.example.cinemaster.dto.response.ApiResponse;
import com.example.cinemaster.entity.Account;
import com.example.cinemaster.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    /* =============================================================
       🔹 CUSTOMER: Gửi yêu cầu hủy vé
    ============================================================= */
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

    /* =============================================================
       🔹 STAFF: Duyệt hủy vé
    ============================================================= */
    // ✅ Staff duyệt hủy vé
    @PutMapping("/{ticketId}/approve-cancel")
    @PreAuthorize("hasAnyRole('Staff','Manager','Admin')")
    public ResponseEntity<ApiResponse<TicketResponse>> approveCancel(
            @PathVariable Integer ticketId,
            @RequestParam Integer accountId) {

        Account staff = new Account();
        staff.setAccountID(accountId); // 👈 set thủ công từ request

        return ResponseEntity.ok(ApiResponse.<TicketResponse>builder()
                .code(1000)
                .message("Đã duyệt hủy vé")
                .result(ticketService.approveCancel(ticketId, staff))
                .build());
    }

    /* =============================================================
       🔹 STAFF: Duyệt hoàn tiền
    ============================================================= */
    // ✅ Staff duyệt hoàn tiền
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

    /* =============================================================
       🔹 STAFF: Danh sách vé chờ hủy
    ============================================================= */
    @GetMapping("/branch/{branchId}/pending-cancel")
    @PreAuthorize("hasRole('Staff')")
    public ResponseEntity<ApiResponse<List<TicketResponse>>> getPendingCancelTickets(@PathVariable Integer branchId) {
        return ResponseEntity.ok(ApiResponse.<List<TicketResponse>>builder()
                .code(1000)
                .message("Danh sách vé chờ hủy")
                .result(ticketService.getPendingCancelTickets(branchId))
                .build());
    }

    /* =============================================================
       🔹 CUSTOMER: Danh sách vé theo tài khoản
    ============================================================= */
    @GetMapping("/account/{accountID}")
    @PreAuthorize("hasRole('Customer')")
    public ResponseEntity<List<TicketResponse>> getTicketsByAccount(@PathVariable Integer accountID) {
        return ResponseEntity.ok(ticketService.getTicketsByAccount(accountID));
    }

    /* =============================================================
       🔹 STAFF: Vé theo chi nhánh
    ============================================================= */
    @GetMapping("/branch/{branchId}")
    @PreAuthorize("hasRole('Staff')")
    public ResponseEntity<ApiResponse<List<TicketResponse>>> getTicketsByBranch(@PathVariable Integer branchId) {
        return ResponseEntity.ok(ApiResponse.<List<TicketResponse>>builder()
                .code(1000)
                .message("Danh sách vé theo chi nhánh")
                .result(ticketService.getTicketsByBranch(branchId))
                .build());
    }

    /* =============================================================
       🔹 STAFF: Cập nhật trạng thái thủ công (nếu cần)
    ============================================================= */
    // ✅ Cập nhật trạng thái thủ công
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

    /* =============================================================
       🔹 Chi tiết vé
    ============================================================= */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TicketDetailResponse>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.<TicketDetailResponse>builder()
                .code(1000)
                .message("Success")
                .result(ticketService.getById(id))
                .build());
    }
}
