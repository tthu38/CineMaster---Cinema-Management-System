package com.example.cinemaster.controller;


import com.example.cinemaster.dto.request.ShiftRequestCreateRequest;
import com.example.cinemaster.dto.response.ShiftRequestResponse;
import com.example.cinemaster.service.ShiftRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


import java.util.List;


@RestController
@RequestMapping("/api/v1/shift-requests")
@RequiredArgsConstructor
public class ShiftRequestController {


    private final ShiftRequestService shiftRequestService;


    /**
     * Nhân viên gửi nhiều yêu cầu ca làm (nhiều shift)
     */
    @PreAuthorize("hasRole('Staff')")
    @PostMapping
    public ResponseEntity<List<ShiftRequestResponse>> createRequest(
            @Valid @RequestBody ShiftRequestCreateRequest request) {


        List<ShiftRequestResponse> response = shiftRequestService.createRequest(request);
        return ResponseEntity.ok(response);
    }


    /**
     * Lấy danh sách request theo chi nhánh
     */
    @PreAuthorize("hasAnyRole('Manager','Admin')")
    @GetMapping("/branch/{branchId}")
    public ResponseEntity<List<ShiftRequestResponse>> getRequestsByBranch(
            @PathVariable Integer branchId) {


        List<ShiftRequestResponse> list = shiftRequestService.getRequestsByBranch(branchId);
        return ResponseEntity.ok(list);
    }


    /**
     * Lấy danh sách request của 1 nhân viên
     */
    @PreAuthorize("hasAnyRole('Staff','Manager','Admin')")
    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<ShiftRequestResponse>> getRequestsByAccount(
            @PathVariable Integer accountId) {


        List<ShiftRequestResponse> list = shiftRequestService.getRequestsByAccount(accountId);
        return ResponseEntity.ok(list);
    }


    /**
     * Manager duyệt / từ chối request
     */
    @PreAuthorize("hasAnyRole('Manager','Admin')")
    @PatchMapping("/{requestId}/status")
    public ResponseEntity<ShiftRequestResponse> updateStatus(
            @PathVariable Integer requestId,
            @RequestParam String status) {


        ShiftRequestResponse updated = shiftRequestService.updateStatus(requestId, status);
        return ResponseEntity.ok(updated);
    }
}



