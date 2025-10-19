// src/main/java/com/example/cinemaster/dto/request/RevenueScope.java
package com.example.cinemaster.dto.request;

public enum RevenueScopeResquest {
    SHIFT,   // theo ca trong 1 ngày
    DAY,     // từng ngày trong 1 tháng
    MONTH,   // từng tháng trong 1 năm
    YEAR     // từng năm (dải nhiều năm)
}
