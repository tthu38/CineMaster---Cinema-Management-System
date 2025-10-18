package com.example.cinemaster.mapper;

import com.example.cinemaster.dto.response.RevenueDayResponse;
import com.example.cinemaster.dto.response.RevenueRowResponse;
import com.example.cinemaster.entity.Payment;
import com.example.cinemaster.repository.projection.RevenueAggregate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RevenueMapper {

    @Mapping(target = "label", ignore = true)
    @Mapping(target = "from", ignore = true)
    @Mapping(target = "to", ignore = true)
    RevenueRowResponse toResponse(RevenueAggregate aggregate);

    @Mapping(
            target = "date",
            expression = "java(p.getCreatedAt() != null ? " +
                    "p.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate() : null)"
    )
    @Mapping(target = "totalRevenue", source = "amount")
    RevenueDayResponse toResponse(Payment p);

    List<RevenueDayResponse> toResponseList(List<Payment> payments);
}
