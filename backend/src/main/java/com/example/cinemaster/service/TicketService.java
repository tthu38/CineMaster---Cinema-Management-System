package com.example.cinemaster.service;

import com.example.cinemaster.dto.response.TicketDetailResponse;
import com.example.cinemaster.dto.response.TicketResponse;
import com.example.cinemaster.entity.*;
import com.example.cinemaster.mapper.TicketMapper;
import com.example.cinemaster.repository.AccountRepository;
import com.example.cinemaster.repository.TicketHistoryRepository;
import com.example.cinemaster.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketMapper ticketMapper;
    private final TicketHistoryRepository ticketHistoryRepository;
    private final AccountRepository accountRepository; // ✅ thêm để load staff thật

    /* =============================================================
       🔹 Tiện ích ghi lịch sử trạng thái (dùng chung)
    ============================================================= */
    private void saveTicketHistory(Ticket ticket, String oldStatus, String newStatus, Account changer, String note) {
        // ⚡ Nếu changer chỉ có ID (transient) => fetch entity thật
        Account realChanger = null;
        if (changer != null && changer.getAccountID() != null) {
            realChanger = accountRepository.findById(changer.getAccountID()).orElse(null);
        }

        TicketHistory history = TicketHistory.builder()
                .ticket(ticket)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .changedBy(realChanger)
                .changedAt(Instant.now())
                .note(note)
                .build();

        ticketHistoryRepository.save(history);
    }

    /* =============================================================
       🔹 CUSTOMER: Gửi yêu cầu hủy vé
    ============================================================= */
    @Transactional
    public TicketResponse requestCancel(Integer ticketId, Account requester) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy vé"));

        String current = ticket.getTicketStatus();
        if (!"Booked".equalsIgnoreCase(current))
            throw new RuntimeException("Chỉ có thể gửi yêu cầu hủy khi vé đang ở trạng thái 'Booked'");

        ticket.setTicketStatus("CancelRequested");
        ticketRepository.save(ticket);

        // ✅ Lấy luôn account từ vé — đảm bảo có ID thật trong DB
        Account realCustomer = ticket.getAccount();

        saveTicketHistory(ticket, current, "CancelRequested", realCustomer, "Khách hàng yêu cầu hủy vé");

        return ticketMapper.toResponse(ticket);
    }




    /* =============================================================
       🔹 STAFF: Duyệt hủy vé
    ============================================================= */
    @Transactional
    public TicketResponse approveCancel(Integer ticketId, Account staff) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy vé"));

        String current = ticket.getTicketStatus();
        if (!"CancelRequested".equalsIgnoreCase(current))
            throw new RuntimeException("Chỉ phê duyệt khi vé ở trạng thái 'CancelRequested'");

        ticket.setTicketStatus("Cancelled");
        ticketRepository.save(ticket);

        saveTicketHistory(ticket, current, "Cancelled", staff, "Nhân viên duyệt hủy vé");

        return ticketMapper.toResponse(ticket);
    }

    /* =============================================================
       🔹 STAFF / ADMIN: Duyệt hoàn tiền vé
    ============================================================= */
    @Transactional
    public TicketResponse approveRefund(Integer ticketId, Account staff) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy vé"));

        String current = ticket.getTicketStatus();
        if (!"Cancelled".equalsIgnoreCase(current))
            throw new RuntimeException("Chỉ hoàn tiền cho vé đã bị hủy");

        ticket.setTicketStatus("Refunded");
        ticketRepository.save(ticket);

        saveTicketHistory(ticket, current, "Refunded", staff, "Nhân viên xác nhận hoàn tiền vé");

        return ticketMapper.toResponse(ticket);
    }

    /* =============================================================
       🔹 STAFF: Danh sách vé chờ hủy
    ============================================================= */
    public List<TicketResponse> getPendingCancelTickets(Integer branchId) {
        return ticketRepository.findByBranch(branchId).stream()
                .filter(t -> "CancelRequested".equalsIgnoreCase(t.getTicketStatus()))
                .map(ticketMapper::toResponse)
                .collect(Collectors.toList());
    }

    /* =============================================================
       🔹 CUSTOMER: Lấy danh sách vé của người dùng
    ============================================================= */
    public List<TicketResponse> getTicketsByAccount(Integer accountID) {
        return ticketRepository.findByAccount_AccountID(accountID).stream()
                .map(ticketMapper::toResponse)
                .collect(Collectors.toList());
    }

    /* =============================================================
       🔹 STAFF: Lấy danh sách vé theo chi nhánh
    ============================================================= */
    public List<TicketResponse> getTicketsByBranch(Integer branchId) {
        return ticketRepository.findByBranch(branchId).stream()
                .map(ticketMapper::toResponse)
                .collect(Collectors.toList());
    }

    /* =============================================================
       🔹 STAFF: Cập nhật trạng thái thủ công (debug / special case)
    ============================================================= */
    @Transactional
    public TicketResponse updateTicketStatus(Integer ticketId, String newStatus, Account staff) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy vé"));

        String oldStatus = ticket.getTicketStatus();
        ticket.setTicketStatus(newStatus);
        ticketRepository.save(ticket);

        saveTicketHistory(ticket, oldStatus, newStatus, staff, "Cập nhật trạng thái thủ công");

        return ticketMapper.toResponse(ticket);
    }

    /* =============================================================
       🔹 Chi tiết vé (có ghế + combo)
    ============================================================= */
    public TicketDetailResponse getById(Integer id) {
        Ticket ticket = ticketRepository.findWithRelationsByTicketID(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy vé"));

        TicketDetailResponse dto = ticketMapper.toDetailResponse(ticket);

        // Ghế
        String seatNums = (ticket.getTicketSeats() != null && !ticket.getTicketSeats().isEmpty())
                ? ticket.getTicketSeats().stream()
                .map(ts -> ts.getSeat().getSeatNumber())
                .collect(Collectors.joining(", "))
                : "N/A";
        dto.setSeatNumbers(seatNums);

        // Combo
        List<String> combos = (ticket.getTicketCombos() != null && !ticket.getTicketCombos().isEmpty())
                ? ticket.getTicketCombos().stream()
                .map(tc -> tc.getCombo().getNameCombo())
                .collect(Collectors.toList())
                : List.of();
        dto.setComboList(combos);

        dto.setTicketStatus(ticket.getTicketStatus());
        dto.setTotalPrice(ticket.getTotalPrice() != null ? ticket.getTotalPrice().doubleValue() : 0.0);
        dto.setPaymentMethod(ticket.getPaymentMethod());

        return dto;
    }
}
