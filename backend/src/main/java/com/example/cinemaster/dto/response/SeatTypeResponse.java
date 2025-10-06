package com.example.cinemaster.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SeatTypeResponse {
    Integer typeID;
    String typeName;
    BigDecimal priceMultiplier;
}
