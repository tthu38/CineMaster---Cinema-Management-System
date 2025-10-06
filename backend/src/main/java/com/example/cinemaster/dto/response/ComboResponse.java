package com.example.cinemaster.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ComboResponse {
    Integer id;
    Integer branchId;
    String branchName;
    String nameCombo;
    BigDecimal price;
    String descriptionCombo;
    String items;
    Boolean available;
    String imageURL;
}
