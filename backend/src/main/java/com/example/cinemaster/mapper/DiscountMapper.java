package com.example.cinemaster.mapper;

import com.example.cinemaster.dto.request.DiscountRequest;
import com.example.cinemaster.dto.response.DiscountResponse;
import com.example.cinemaster.entity.Discount;
import org.mapstruct.*;
import java.time.LocalDate;

@Mapper(componentModel = "spring")
public interface DiscountMapper {

    Discount toEntity(DiscountRequest request);

    DiscountResponse toResponse(Discount entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateDiscountFromRequest(DiscountRequest request, @MappingTarget Discount discount);

    @AfterMapping
    default void setCreateAt(@MappingTarget Discount discount) {
        if (discount.getCreateAt() == null) {
            discount.setCreateAt(LocalDate.now());
        }
    }
}
