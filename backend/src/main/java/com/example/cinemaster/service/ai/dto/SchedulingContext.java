        package com.example.cinemaster.service.ai.dto;

import com.example.cinemaster.entity.Account;
import com.example.cinemaster.entity.ShiftRequest;
import lombok.*;

        import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * SchedulingContext chứa toàn bộ dữ liệu AI cần
 * để sinh lịch làm (Genetic Algorithm + SA + RL)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchedulingContext {

    // Chi nhánh cần tạo lịch
    private Integer branchId;

    // Ngày bắt đầu tuần (Monday)
    private LocalDate weekStart;

    // Danh sách nhân viên trong chi nhánh
    private List<Account> staff;

    // Danh sách yêu cầu ca làm của staff
    private List<ShiftRequest> shiftRequests;

    // Số lượng nhân sự cần cho mỗi ca
    // Ví dụ:
    // key = "MONDAY_MORNING", value = 3
    private Map<String, Integer> requiredPerShift;

    // Các ca trong ngày
    private List<String> shiftTypes;
    // Ví dụ: ["MORNING", "AFTERNOON", "NIGHT"]

    // 7 ngày trong tuần
    private List<LocalDate> weekDates;
}

