package com.example.cinemaster.repository;

import com.example.cinemaster.entity.WorkSchedule;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface WorkScheduleRepository
        extends JpaRepository<WorkSchedule, Integer>, JpaSpecificationExecutor<WorkSchedule> {

    @Modifying
    @Query("""
           DELETE FROM WorkSchedule ws
           WHERE ws.branch.id = :branchId
             AND ws.shiftDate = :date
             AND ws.shiftType = :shiftType
           """)
    void deleteCell(@Param("branchId") Integer branchId,
                    @Param("date") LocalDate date,
                    @Param("shiftType") String shiftType);

    // Lấy tất cả record trong 1 ô
    List<WorkSchedule> findByBranch_IdAndShiftDateAndShiftType(Integer branchId,
                                                               LocalDate date,
                                                               String shiftType);

    // ---- Specifications dùng cho search ----
    static Specification<WorkSchedule> hasAccount(Integer accountId) {
        return (root, q, cb) -> accountId == null ? cb.conjunction()
                : cb.equal(root.get("account").get("accountID"), accountId);
    }

    static Specification<WorkSchedule> hasBranch(Integer branchId) {
        return (root, q, cb) -> branchId == null ? cb.conjunction()
                : cb.equal(root.get("branch").get("id"), branchId);
    }

    static Specification<WorkSchedule> dateBetween(LocalDate from, LocalDate to) {
        return (root, q, cb) -> {
            if (from == null && to == null) return cb.conjunction();
            if (from != null && to != null) return cb.between(root.get("shiftDate"), from, to);
            return from != null
                    ? cb.greaterThanOrEqualTo(root.get("shiftDate"), from)
                    : cb.lessThanOrEqualTo(root.get("shiftDate"), to);
        };
    }

    // Kiểm tra overlap
    boolean existsByAccount_AccountIDAndShiftDateAndStartTimeLessThanAndEndTimeGreaterThan(
            Integer accountId, LocalDate shiftDate, LocalTime newEnd, LocalTime newStart);

    boolean existsByAccount_AccountIDAndShiftDateAndStartTimeLessThanAndEndTimeGreaterThanAndIdNot(
            Integer accountId, LocalDate shiftDate, LocalTime newEnd, LocalTime newStart, Integer excludeId);
}
