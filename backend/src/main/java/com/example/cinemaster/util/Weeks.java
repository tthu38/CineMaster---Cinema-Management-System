// src/main/java/com/example/cinemaster/util/Weeks.java
package com.example.cinemaster.util;

import java.time.*;

public final class Weeks {
    private Weeks(){}

    public static record Range(LocalDateTime start, LocalDateTime end){}

    /** Tuần chứa anchor (Mon 00:00 -> Mon+7 00:00), theo zone */
    public static Range weekOf(LocalDate anchor, ZoneId zone){
        LocalDate monday = anchor.with(DayOfWeek.MONDAY);
        return new Range(monday.atStartOfDay(), monday.plusDays(7).atStartOfDay());
    }

    /** Tuần hiện tại (theo zone) */
    public static Range thisWeek(ZoneId zone){
        return weekOf(LocalDate.now(zone), zone);
    }

    /** Tuần kế tiếp (đang dùng cho create/update) */
    public static Range nextWeek(ZoneId zone){
        LocalDate monday = LocalDate.now(zone).with(DayOfWeek.MONDAY).plusWeeks(1);
        return new Range(monday.atStartOfDay(), monday.plusDays(7).atStartOfDay());
    }
}
