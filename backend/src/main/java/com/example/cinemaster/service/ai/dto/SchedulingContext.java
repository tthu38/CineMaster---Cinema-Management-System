package com.example.cinemaster.service.ai.dto;


import com.example.cinemaster.entity.Account;
import com.example.cinemaster.entity.ShiftRequest;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;


import java.time.LocalDate;
import java.util.*;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchedulingContext {


    private Integer branchId;
    private LocalDate weekStart;


    private List<Account> staff;
    private List<ShiftRequest> shiftRequests;


    private Map<String, Integer> requiredPerShift;
    private List<String> shiftTypes;
    private List<LocalDate> weekDates;


    /** ⭐ Cache lookup request để GA/SA chạy nhanh hơn */
    @JsonIgnore
    @Builder.Default
    private Map<String, Boolean> requestCache = new HashMap<>();


    public boolean hasRequestedShift(Integer staffId, String date, String shiftType) {


        String key = staffId + "_" + date + "_" + shiftType.toUpperCase();


        if (requestCache.containsKey(key))
            return requestCache.get(key);


        boolean ok = shiftRequests != null && shiftRequests.stream()
                .anyMatch(r ->
                        r.getAccount().getAccountID().equals(staffId)
                                && r.getShiftDate().toString().equals(date)
                                && r.getShiftType().equalsIgnoreCase(shiftType)
                );


        requestCache.put(key, ok);
        return ok;
    }
}



