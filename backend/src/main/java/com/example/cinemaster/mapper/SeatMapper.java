package com.example.cinemaster.mapper;

import com.example.cinemaster.dto.request.SeatRequest;
import com.example.cinemaster.dto.response.SeatResponse;
import com.example.cinemaster.entity.Auditorium;
import com.example.cinemaster.entity.Seat;
import com.example.cinemaster.entity.SeatType;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface SeatMapper {

    // ====== 1️⃣ Entity → Response DTO ======
    @Mappings({
            @Mapping(target = "auditoriumID", source = "auditorium.auditoriumID"),
            @Mapping(target = "auditoriumName", source = "auditorium.name"),
            @Mapping(target = "typeID", source = "seatType.typeID"),
            @Mapping(target = "typeName", source = "seatType.typeName"),
            @Mapping(target = "status", expression = "java(seat.getStatus() != null ? seat.getStatus().name() : null)"),
            @Mapping(target = "branchID", source = "auditorium.branch.id"),
            @Mapping(target = "branchName", source = "auditorium.branch.branchName")
    })
    SeatResponse toResponse(Seat seat);

    // ====== 2️⃣ Request → Entity (CREATE) ======
    @Mapping(target = "seatID", ignore = true)
    @Mapping(target = "auditorium", expression = "java(auditorium)")
    @Mapping(target = "seatType", expression = "java(seatType)")
    @Mapping(target = "status", expression = "java(com.example.cinemaster.entity.Seat.SeatStatus.valueOf(request.getStatus().toUpperCase()))")
    Seat toEntity(SeatRequest request, Auditorium auditorium, SeatType seatType);

    // ====== 3️⃣ Request → Entity (UPDATE) ======
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "auditorium", expression = "java(auditorium)")
    @Mapping(target = "seatType", expression = "java(seatType)")
    @Mapping(target = "status", expression = "java(com.example.cinemaster.entity.Seat.SeatStatus.valueOf(request.getStatus().toUpperCase()))")
    void updateEntityFromRequest(SeatRequest request, Auditorium auditorium, SeatType seatType, @MappingTarget Seat seat);
}
