package com.example.cinemaster.service;


import com.example.cinemaster.dto.response.ShiftReportResponse;
import com.example.cinemaster.entity.*;
import com.example.cinemaster.repository.*;
import com.example.cinemaster.security.AccountPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;


@Service
@RequiredArgsConstructor
public class ShiftService {


    private final ShiftSessionRepository sessionRepo;
    private final TicketRepository ticketRepo;
    private final TicketComboRepository comboRepo;
    private final TicketDiscountRepository discountRepo;


    /* ============================================================
       🟢 MỞ CA
    ============================================================ */
    public ShiftSession openShift(AccountPrincipal staff, BigDecimal openingCash) {
        // Nếu nhân viên đã có ca mở, chặn lại
        sessionRepo.findActiveSession(staff.getId()).ifPresent(s -> {
            throw new IllegalStateException("Bạn đang có ca làm chưa kết thúc!");
        });


        ShiftSession session = ShiftSession.builder()
                .staff(Account.builder().accountID(staff.getId()).build()) // chỉ cần ID
                .startTime(LocalDateTime.now())
                .openingCash(openingCash)
                .status("OPEN")
                .build();


        System.out.println("🟢 Mở ca thành công cho nhân viên: " + staff.getFullName() +
                " | ID=" + staff.getId() + " | OpeningCash=" + openingCash);
        return sessionRepo.save(session);
    }


    /* ============================================================
       📊 XEM DOANH THU CA HIỆN TẠI (nhân viên khác cùng branch cũng xem được)
    ============================================================ */
    public ShiftReportResponse getCurrentShiftReport(AccountPrincipal staff) {
        System.out.println("📊 Lấy báo cáo ca cho staffID=" + staff.getId());


        ShiftSession session = sessionRepo.findActiveSession(staff.getId())
                .or(() -> sessionRepo.findAnyOpenSessionInSameBranch(staff.getBranchId()))
                .orElseThrow(() -> new IllegalStateException("Chưa có ca mở trong chi nhánh này!"));


        try {
            LocalDateTime from = session.getStartTime();
            LocalDateTime to = LocalDateTime.now();


            List<Ticket> tickets = ticketRepo.findAllByStaffAndTimeRange(staff.getId(), from, to);
            if (tickets == null) tickets = List.of();


            int soldSeats = tickets.stream()
                    .mapToInt(t -> t.getTicketSeats() != null ? t.getTicketSeats().size() : 0)
                    .sum();


            BigDecimal ticketRevenue = tickets.stream()
                    .map(t -> safe(t.getTotalPrice()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);


            int soldCombos = comboRepo.countCombosByStaffAndTime(staff.getId(), from, to);
            BigDecimal comboRevenue = safe(comboRepo.sumComboRevenueByStaffAndTime(staff.getId(), from, to));
            BigDecimal discountTotal = safe(discountRepo.sumDiscountByStaffAndTime(staff.getId(), from, to));
            BigDecimal revenueCash = safe(ticketRepo.sumRevenueByPaymentMethod(staff.getId(), from, to, "CASH"));
            BigDecimal revenueTransfer = safe(ticketRepo.sumRevenueExceptPaymentMethod(staff.getId(), from, to, "CASH"));


            return new ShiftReportResponse(
                    session.getOpeningCash(),
                    soldSeats, ticketRevenue,
                    soldCombos, comboRevenue,
                    discountTotal,
                    revenueCash, revenueTransfer
            );


        } catch (Exception e) {
            System.out.println("⚠️ Không thể tạo báo cáo, trả mặc định 0. " + e.getMessage());
            return new ShiftReportResponse(
                    session.getOpeningCash(),
                    0, BigDecimal.ZERO,
                    0, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO
            );
        }
    }


    /* ============================================================
       🔴 KẾT CA
    ============================================================ */
    public ShiftSession closeShift(AccountPrincipal staff, BigDecimal closingCash) {
        System.out.println("🔴 Bắt đầu kết ca cho nhân viên: " + staff.getFullName() +
                " | ID=" + staff.getId() + " | ClosingCash=" + closingCash);


        // Tìm ca đang mở: của nhân viên hoặc của chi nhánh
        ShiftSession session = sessionRepo.findActiveSession(staff.getId())
                .or(() -> sessionRepo.findAnyOpenSessionInSameBranch(staff.getBranchId()))
                .orElseThrow(() -> new IllegalStateException("Không có ca làm đang mở."));


        // Nếu không phải người mở ca và không phải manager → chặn
        if (!staff.isManager() && !session.getStaff().getAccountID().equals(staff.getId())) {
            throw new IllegalStateException("Bạn không có quyền kết ca này!");
        }


        // ✅ Nếu không có dữ liệu vé, vẫn cho kết ca bình thường
        ShiftReportResponse report;
        try {
            report = getCurrentShiftReport(staff);
        } catch (Exception e) {
            System.out.println("⚠️ Không thể tạo báo cáo, dùng giá trị mặc định (0). Lỗi: " + e.getMessage());
            report = new ShiftReportResponse(
                    session.getOpeningCash(), 0, BigDecimal.ZERO,
                    0, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO
            );
        }


        BigDecimal expectedCash = report.getRevenueCash().add(session.getOpeningCash());
        BigDecimal diff = closingCash.subtract(expectedCash);


        session.setEndTime(LocalDateTime.now());
        session.setClosingCash(closingCash);
        session.setExpectedCash(expectedCash);
        session.setDifference(diff);
        session.setStatus("CLOSED");


        ShiftSession saved = sessionRepo.save(session);
        System.out.println("✅ Kết ca thành công! SessionID=" + saved.getId() +
                " | ExpectedCash=" + expectedCash + " | Diff=" + diff);


        return saved;
    }


    /* ============================================================
       ⚙️ HÀM TIỆN ÍCH
    ============================================================ */
    private BigDecimal safe(BigDecimal val) {
        return val != null ? val : BigDecimal.ZERO;
    }
}

