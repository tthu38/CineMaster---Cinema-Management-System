package com.example.cinemaster.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ComboRequest {
    Integer branchId;
    String nameCombo;
    BigDecimal price;
    String descriptionCombo;
    String items;
    Boolean available;
    String imageURL;
}
