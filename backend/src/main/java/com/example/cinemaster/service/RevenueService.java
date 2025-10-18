package com.example.cinemaster.service;

import com.example.cinemaster.dto.request.RevenueQueryResquest;
import com.example.cinemaster.dto.request.RevenueScopeResquest;
import com.example.cinemaster.dto.response.RevenueDayResponse;
import com.example.cinemaster.dto.response.RevenueRowResponse;
import com.example.cinemaster.entity.Payment;
import com.example.cinemaster.entity.WorkSchedule;
import com.example.cinemaster.mapper.RevenueMapper;
import com.example.cinemaster.repository.*;
import com.example.cinemaster.repository.projection.RevenueAggregate;
import com.example.cinemaster.security.AccountPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RevenueService {

    private final RevenueRepository repo;
    private final RevenueMapper mapper;
    private final WorkScheduleRepository scheduleRepo;
    private final AccountRepository accountRepo;

    /* ============================================================
       🟦 LẤY DOANH THU CHUNG (CHO DASHBOARD)
    ============================================================ */
    public List<RevenueDayResponse> getRevenue(AccountPrincipal user) {
        LocalDate today = LocalDate.now();

        if (user.hasRole("Admin")) {
            LocalDate from = today.minusMonths(3);
            return aggregateByDate(
                    repo.findAllInRange(from.atStartOfDay().toInstant(ZoneOffset.UTC),
                            today.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC))
            );
        }

        if (user.hasRole("Manager")) {
            YearMonth currentMonth = YearMonth.now();
            LocalDate start = currentMonth.atDay(1);
            LocalDate end = currentMonth.atEndOfMonth();
            return aggregateByDate(
                    repo.findAllByBranchInRange(user.getBranchId(),
                            start.atStartOfDay().toInstant(ZoneOffset.UTC),
                            end.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC))
            );
        }

        if (user.hasRole("Staff")) {
            List<Payment> payments = repo.findAllByBranchAndDate(user.getBranchId(), today);
            return aggregateByDate(payments);
        }

        throw new SecurityException("Không có quyền xem thống kê doanh thu.");
    }

    private List<RevenueDayResponse> aggregateByDate(List<Payment> payments) {
        Map<LocalDate, BigDecimal> grouped = payments.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate(),
                        Collectors.reducing(BigDecimal.ZERO, Payment::getAmount, BigDecimal::add)
                ));

        return grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new RevenueDayResponse(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    /* ============================================================
       🟩 BÁO CÁO CHI TIẾT (CA / NGÀY / THÁNG / NĂM)
    ============================================================ */

    public List<RevenueRowResponse> getReport(RevenueQueryResquest q, AccountPrincipal user) {
        enforceScopeByRole(q, user);
        Integer branchId = normalizeBranch(q, user);

        return switch (q.getScope()) {
            case SHIFT -> reportByShiftFromSchedule(q.getAnchorDate(), branchId);
            case DAY -> reportByDayOfMonth(q.getAnchorDate(), branchId);
            case MONTH -> reportByMonthOfYear(q.getYear(), branchId);
            case YEAR -> reportByYearRange(q.getFromYear(), q.getToYear(), branchId);
        };
    }

    /* ==================== 1️⃣ THEO CA (DÙNG WORKSCHEDULE) ==================== */
    private List<RevenueRowResponse> reportByShiftFromSchedule(LocalDate date, Integer branchId) {
        LocalDate targetDate = (date != null) ? date : LocalDate.now();

        // Lấy danh sách ca làm việc theo chi nhánh và ngày
        List<WorkSchedule> shifts = scheduleRepo.findDistinctShiftTypesByDateAndBranch(targetDate, branchId);

        List<RevenueRowResponse> rows = new ArrayList<>();
        for (WorkSchedule s : shifts) {
            LocalDateTime from = LocalDateTime.of(targetDate, s.getStartTime());
            LocalDateTime to = s.getEndTime().isAfter(s.getStartTime())
                    ? LocalDateTime.of(targetDate, s.getEndTime())
                    : LocalDateTime.of(targetDate.plusDays(1), s.getEndTime());

            rows.add(buildRow(
                    s.getShiftType() + " (" + s.getStartTime() + "–" + s.getEndTime() + ")",
                    from, to, branchId
            ));
        }

        // Nếu không có dữ liệu lịch làm việc → fallback 3 ca mặc định
        if (rows.isEmpty()) {
            rows = List.of(
                    buildRow("MORNING (08:00–14:00)", targetDate.atTime(8, 0), targetDate.atTime(14, 0), branchId),
                    buildRow("AFTERNOON (14:00–22:00)", targetDate.atTime(14, 0), targetDate.atTime(22, 0), branchId),
                    buildRow("NIGHT (22:00–08:00)", targetDate.atTime(22, 0), targetDate.plusDays(1).atTime(8, 0), branchId)
            );
        }

        return rows;
    }

    /* ==================== 2️⃣ THEO NGÀY ==================== */
    private List<RevenueRowResponse> reportByDayOfMonth(LocalDate anchor, Integer branchId) {
        LocalDate base = (anchor != null) ? anchor : LocalDate.now();
        LocalDate first = base.withDayOfMonth(1);
        LocalDate last = base.withDayOfMonth(base.lengthOfMonth());

        List<RevenueRowResponse> rows = new ArrayList<>();
        for (LocalDate d = first; !d.isAfter(last); d = d.plusDays(1)) {
            rows.add(buildRow(d.toString(), d.atStartOfDay(), d.plusDays(1).atStartOfDay(), branchId));
        }
        return rows;
    }

    /* ==================== 3️⃣ THEO THÁNG ==================== */
    private List<RevenueRowResponse> reportByMonthOfYear(Integer year, Integer branchId) {
        int y = (year != null) ? year : Year.now().getValue();
        List<RevenueRowResponse> rows = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            LocalDate first = LocalDate.of(y, m, 1);
            LocalDateTime from = first.atStartOfDay();
            LocalDateTime to = first.plusMonths(1).atStartOfDay();
            rows.add(buildRow("Tháng " + m + "/" + y, from, to, branchId));
        }
        return rows;
    }

    /* ==================== 4️⃣ THEO NĂM ==================== */
    private List<RevenueRowResponse> reportByYearRange(Integer fromYear, Integer toYear, Integer branchId) {
        int y1 = (fromYear != null) ? fromYear : Year.now().getValue();
        int y2 = (toYear != null && toYear >= y1) ? toYear : y1;

        List<RevenueRowResponse> rows = new ArrayList<>();
        for (int y = y1; y <= y2; y++) {
            LocalDateTime from = LocalDate.of(y, 1, 1).atStartOfDay();
            LocalDateTime to = LocalDate.of(y + 1, 1, 1).atStartOfDay();
            rows.add(buildRow("Năm " + y, from, to, branchId));
        }
        return rows;
    }

    /* ==================== 🧮 BUILD ROW ==================== */
    private RevenueRowResponse buildRow(String label, LocalDateTime from, LocalDateTime to, Integer branchId) {
        RevenueAggregate aggr = repo.aggregateForWindow(from, to, branchId);
        RevenueRowResponse row = mapper.toResponse(aggr);

        if (row == null) row = new RevenueRowResponse();
        row.setLabel(label);
        row.setFrom(from);
        row.setTo(to);

        if (row.getTotalRevenue() == null) row.setTotalRevenue(BigDecimal.ZERO);
        if (row.getTicketRevenue() == null) row.setTicketRevenue(BigDecimal.ZERO);
        if (row.getComboRevenue() == null) row.setComboRevenue(BigDecimal.ZERO);
        if (row.getDiscountTotal() == null) row.setDiscountTotal(BigDecimal.ZERO);
        if (row.getRevenueOnline() == null) row.setRevenueOnline(BigDecimal.ZERO);
        if (row.getRevenueCash() == null) row.setRevenueCash(BigDecimal.ZERO);

        return row;
    }

    /* ============================================================
       🟨 PHÂN QUYỀN & CHI NHÁNH
    ============================================================ */
    private void enforceScopeByRole(RevenueQueryResquest q, AccountPrincipal user) {
        if (user.hasRole("Admin")) return;

        if (user.hasRole("Manager")) {
            if (q.getScope() == RevenueScopeResquest.MONTH || q.getScope() == RevenueScopeResquest.YEAR) {
                throw new SecurityException("Manager chỉ được xem theo ca hoặc ngày.");
            }
            if (q.getAnchorDate() == null) q.setAnchorDate(LocalDate.now());
            return;
        }

        if (user.hasRole("Staff")) {
            if (q.getScope() != RevenueScopeResquest.SHIFT) {
                throw new SecurityException("Staff chỉ được xem theo ca trong ngày.");
            }
            q.setAnchorDate(LocalDate.now());
        }
    }

    private Integer normalizeBranch(RevenueQueryResquest q, AccountPrincipal user) {
        // ✅ Admin có thể chọn branch tùy ý
        if (user.hasRole("Admin")) {
            if (q.getBranchId() != null) return q.getBranchId();
        }

        // ✅ Manager hoặc Staff
        if (user.hasRole("Manager") || user.hasRole("Staff")) {
            Integer branchId = user.getBranchId();
            if (branchId == null) {
                branchId = getBranchIdFromDatabase(user.getId());
                System.out.println("DEBUG ⚡ Auto-loaded branchId=" + branchId + " for userId=" + user.getId());
            }
            return branchId;
        }

        return null;
    }

    private Integer getBranchIdFromDatabase(Integer accountId) {
        return accountRepo.findById(accountId)
                .map(a -> a.getBranch() != null ? a.getBranch().getId() : null)
                .orElse(null);
    }
}
