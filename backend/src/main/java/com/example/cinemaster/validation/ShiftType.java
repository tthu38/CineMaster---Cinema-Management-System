package com.example.cinemaster.validation;

import java.time.*;
import java.time.format.DateTimeFormatter;

public enum ShiftType {
    MORNING(LocalTime.of(8, 0),  LocalTime.of(12, 0)),
    AFTERNOON(LocalTime.of(13, 0), LocalTime.of(17, 0)),
    NIGHT(LocalTime.of(18, 0),    LocalTime.of(22, 0));

    public final LocalTime start;
    public final LocalTime end;

    ShiftType(LocalTime start, LocalTime end) {
        this.start = start;
        this.end = end;
    }

    public static ShiftType fromAction(String action) {
        if (action == null) return null;
        return ShiftType.valueOf(action.trim().toUpperCase());
    }

    public static Instant[] rangeForDay(Instant anyTimeInDay, ZoneId zone, ShiftType st) {
        ZonedDateTime zdt = anyTimeInDay.atZone(zone);
        LocalDate d = zdt.toLocalDate();
        ZonedDateTime start = ZonedDateTime.of(d, st.start, zone);
        ZonedDateTime end   = ZonedDateTime.of(d, st.end,   zone);
        return new Instant[]{ start.toInstant(), end.toInstant() };
    }

    public static String hhmm(LocalTime t) {
        return t.format(DateTimeFormatter.ofPattern("HH:mm"));
    }
}
