package com.example.cinemaster.service;

import com.example.cinemaster.dto.request.RevenueQueryResquest;
import com.example.cinemaster.dto.request.RevenueScopeResquest;
import com.example.cinemaster.dto.response.RevenueDayResponse;
import com.example.cinemaster.dto.response.RevenueRowResponse;
import com.example.cinemaster.repository.*;
import com.example.cinemaster.security.AccountPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RevenueService {

    private final TicketRepository ticketRepo;
    private final ComboRepository comboRepo;
    private final DiscountRepository discountRepo;
    private final AccountRepository accountRepo;

    /* =====================================================
        DOANH THU TỔNG HỢP (DASHBOARD)
    ===================================================== */
    @Transactional(readOnly = true)
    public List<RevenueDayResponse> getRevenue(AccountPrincipal user) {
        LocalDate today = LocalDate.now();
        LocalDate from;
        LocalDate to = today.plusDays(1);

        Integer branchId = user.isAdmin() ? null : user.getBranchId();

        if (user.isAdmin()) {
            from = today.minusDays(90);
        } else if (user.isManager()) {
            from = YearMonth.now().atDay(1);
        } else {
            from = today.minusDays(7);
        }

        List<Object[]> rows = ticketRepo.findRevenueBetweenDates(from, today, branchId);
        Map<LocalDate, BigDecimal> map = new HashMap<>();
        for (Object[] row : rows) {
            LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
            BigDecimal amount = (BigDecimal) row[1];
            map.put(date, amount);
        }

        List<RevenueDayResponse> result = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(today); d = d.plusDays(1)) {
            result.add(new RevenueDayResponse(d, map.getOrDefault(d, BigDecimal.ZERO)));
        }
        return result;
    }

    /* =====================================================
       BÁO CÁO THEO PHẠM VI (Ngày / Tháng / Năm)
    ===================================================== */
    public List<RevenueRowResponse> getReport(RevenueQueryResquest q, AccountPrincipal user) {
        enforceScopeByRole(q, user);
        Integer branchId = normalizeBranch(q, user);

        return switch (q.getScope()) {
            case DAY -> reportByDayOfMonth(q.getAnchorDate(), branchId);
            case MONTH -> reportByMonthOfYear(q.getYear(), branchId);
            case YEAR -> reportByYearRange(q.getFromYear(), q.getToYear(), branchId);
            case CUSTOM -> reportByCustomRange(q.getFrom(), q.getTo(), branchId);
        };

    }
    private List<RevenueRowResponse> reportByCustomRange(LocalDate from, LocalDate to, Integer branchId) {
        LocalDate safeFrom = (from != null) ? from : LocalDate.now().minusDays(7);
        LocalDate safeTo = (to != null && !to.isBefore(safeFrom)) ? to : LocalDate.now();

        List<RevenueRowResponse> rows = new ArrayList<>();
        for (LocalDate d = safeFrom; !d.isAfter(safeTo); d = d.plusDays(1)) {
            LocalDateTime start = d.atStartOfDay();
            LocalDateTime end = d.plusDays(1).atStartOfDay();
            rows.add(calculateRevenue("Ngày " + d.getDayOfMonth() + "/" + d.getMonthValue(), start, end, branchId));
        }
        return rows;
    }

    /* =====================================================
        THEO NGÀY TRONG THÁNG
    ===================================================== */
    private List<RevenueRowResponse> reportByDayOfMonth(LocalDate anchor, Integer branchId) {
        LocalDate base = (anchor != null) ? anchor : LocalDate.now();
        LocalDate first = base.withDayOfMonth(1);
        LocalDate last = base.withDayOfMonth(base.lengthOfMonth());

        List<RevenueRowResponse> rows = new ArrayList<>();
        for (LocalDate d = first; !d.isAfter(last); d = d.plusDays(1)) {
            rows.add(calculateRevenue("Ngày " + d.getDayOfMonth(), d.atStartOfDay(), d.plusDays(1).atStartOfDay(), branchId));
        }
        return rows;
    }

    /* =====================================================
       THEO THÁNG TRONG NĂM
    ===================================================== */
    private List<RevenueRowResponse> reportByMonthOfYear(Integer year, Integer branchId) {
        int y = (year != null) ? year : Year.now().getValue();
        List<RevenueRowResponse> rows = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            LocalDate first = LocalDate.of(y, m, 1);
            LocalDateTime from = first.atStartOfDay();
            LocalDateTime to = first.plusMonths(1).atStartOfDay();
            rows.add(calculateRevenue("Tháng " + m + "/" + y, from, to, branchId));
        }
        return rows;
    }

    /* =====================================================
       📅 THEO NĂM
    ===================================================== */
    private List<RevenueRowResponse> reportByYearRange(Integer fromYear, Integer toYear, Integer branchId) {
        int y1 = (fromYear != null) ? fromYear : Year.now().getValue();
        int y2 = (toYear != null && toYear >= y1) ? toYear : y1;

        List<RevenueRowResponse> rows = new ArrayList<>();
        for (int y = y1; y <= y2; y++) {
            LocalDateTime from = LocalDate.of(y, 1, 1).atStartOfDay();
            LocalDateTime to = LocalDate.of(y + 1, 1, 1).atStartOfDay();
            rows.add(calculateRevenue("Năm " + y, from, to, branchId));
        }
        return rows;
    }

    private RevenueRowResponse calculateRevenue(String label, LocalDateTime from, LocalDateTime to, Integer branchId) {

        Long ticketsSold = safeLong(ticketRepo.countSeatsSold(from, to, branchId));

        Long combosSold = safeLong(comboRepo.countCombosSold(from, to, branchId));

        BigDecimal totalRevenue = safe(ticketRepo.getTicketRevenue(from, to, branchId));

        BigDecimal discountTotal = safe(discountRepo.getDiscountTotal(from, to, branchId));

        BigDecimal grossBeforeDiscount = totalRevenue.add(discountTotal);

        BigDecimal revenueOnline = safe(ticketRepo.getRevenueByMethod(from, to, branchId, "BANKING"));
        BigDecimal revenueCash   = safe(ticketRepo.getRevenueByMethod(from, to, branchId, "CASH"));

        return RevenueRowResponse.builder()
                .label(label)
                .from(from)
                .to(to)
                .ticketsSold(ticketsSold)
                .combosSold(combosSold)
                .grossBeforeDiscount(grossBeforeDiscount)
                .discountTotal(discountTotal)
                .revenueOnline(revenueOnline)
                .revenueCash(revenueCash)
                .totalRevenue(totalRevenue)
                .build();
    }



    private BigDecimal safe(BigDecimal val) {
        return val != null ? val : BigDecimal.ZERO;
    }

    private Long safeLong(Long val) {
        return val != null ? val : 0L;
    }

    /* =====================================================
        PHÂN QUYỀN & CHI NHÁNH
    ===================================================== */
    private void enforceScopeByRole(RevenueQueryResquest q, AccountPrincipal user) {
        if (user.hasRole("Admin")) return;

        if (user.hasRole("Manager")) {
            if (q.getScope() == RevenueScopeResquest.MONTH || q.getScope() == RevenueScopeResquest.YEAR) {
                throw new SecurityException("Manager chỉ được xem theo ngày.");
            }
            if (q.getAnchorDate() == null) q.setAnchorDate(LocalDate.now());
        }
    }

    private Integer normalizeBranch(RevenueQueryResquest q, AccountPrincipal user) {
        if (user.hasRole("Admin")) return q.getBranchId();

        Integer branchId = user.getBranchId();
        if (branchId == null) {
            branchId = accountRepo.findById(user.getId())
                    .map(a -> a.getBranch() != null ? a.getBranch().getId() : null)
                    .orElse(null);
        }
        return branchId;
    }

    /* =====================================================
        DOANH THU 7 NGÀY GẦN NHẤT
    ===================================================== */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRevenueLast7Days(AccountPrincipal principal, Integer branchId) {
        if (principal == null) throw new SecurityException("Không xác thực được người dùng.");
        if (!principal.isAdmin() && !principal.isManager())
            throw new SecurityException("Không quyền truy cập chức năng này.");

        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(6);
        Integer finalBranchId = principal.isAdmin() ? branchId : principal.getBranchId();

        List<Object[]> rows = ticketRepo.findRevenueBetweenDates(from, today, finalBranchId);
        Map<LocalDate, Long> map = new HashMap<>();

        for (Object[] r : rows) {
            LocalDate date = ((java.sql.Date) r[0]).toLocalDate();
            Long amount = ((BigDecimal) r[1]).longValue();
            map.put(date, amount);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(today); d = d.plusDays(1)) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("date", d.toString());
            entry.put("revenue", map.getOrDefault(d, 0L));
            result.add(entry);
        }
        return result;
    }

    /* =====================================================
        TOP 10 PHIM ĐƯỢC MUA VÉ NHIỀU NHẤT
    ===================================================== */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTop10Movies(AccountPrincipal principal,
                                                    Integer branchId,
                                                    LocalDate from,
                                                    LocalDate to,
                                                    Integer year,
                                                    Integer month) {
        if (principal == null)
            throw new SecurityException("Không xác thực được người dùng.");
        if (!principal.isAdmin() && !principal.isManager())
            throw new SecurityException("Không quyền truy cập chức năng này.");

        Integer finalBranchId = principal.isAdmin() ? branchId : principal.getBranchId();
        if (finalBranchId == null)
            finalBranchId = accountRepo.findById(principal.getId())
                    .map(a -> a.getBranch() != null ? a.getBranch().getId() : null)
                    .orElse(null);

        LocalDateTime fromTime = null, toTime = null;
        if (from != null && to != null) {
            fromTime = from.atStartOfDay();
            toTime = to.plusDays(1).atStartOfDay();
        } else if (year != null && month != null) {
            fromTime = LocalDate.of(year, month, 1).atStartOfDay();
            toTime = fromTime.plusMonths(1);
        }

        List<Object[]> rows = ticketRepo.findTop10MoviesByTickets(finalBranchId, fromTime, toTime);

        return rows.stream()
                .limit(10)
                .map(r -> Map.of(
                        "movieTitle", r[0],
                        "ticketsSold", ((Long) r[1])
                ))
                .collect(Collectors.toList());
    }

}
