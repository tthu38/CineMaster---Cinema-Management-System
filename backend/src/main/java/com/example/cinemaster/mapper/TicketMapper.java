package com.example.cinemaster.mapper;

import com.example.cinemaster.dto.response.TicketDetailResponse;
import com.example.cinemaster.dto.response.TicketResponse;
import com.example.cinemaster.entity.Ticket;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface TicketMapper {

    TicketMapper INSTANCE = Mappers.getMapper(TicketMapper.class);

    // ========================== Danh sách vé (rút gọn) ==========================
    @Mapping(target = "ticketId", source = "ticketID")
    @Mapping(target = "movieTitle", source = "showtime.period.movie.title")
    @Mapping(target = "showtimeStart", source = "showtime.startTime")
    @Mapping(target = "branchName", source = "showtime.period.branch.branchName")
    @Mapping(target = "customerName", source = "account.fullName")
    @Mapping(target = "seatNumbers", expression = "java(getSeatNumbers(entity))")
    TicketResponse toResponse(Ticket entity);

    // ========================== Chi tiết vé (đầy đủ) ==========================
    @Mapping(target = "ticketId", source = "ticketID")
    @Mapping(target = "movieTitle", source = "showtime.period.movie.title")
    @Mapping(target = "movieGenre", source = "showtime.period.movie.genre")
    @Mapping(target = "movieDuration", source = "showtime.period.movie.duration")
    @Mapping(target = "showtimeStart", source = "showtime.startTime")
    @Mapping(target = "showtimeEnd", source = "showtime.endTime")
    @Mapping(target = "branchName", source = "showtime.period.branch.branchName")
    @Mapping(target = "auditoriumName", source = "showtime.auditorium.name")
    @Mapping(target = "seatNumbers", expression = "java(getSeatNumbers(entity))")
    @Mapping(target = "comboList", ignore = true)
    TicketDetailResponse toDetailResponse(Ticket entity);

    // ========================== Helper ==========================
    default String getSeatNumbers(Ticket ticket) {
        if (ticket.getTicketSeats() == null || ticket.getTicketSeats().isEmpty()) return "-";
        return ticket.getTicketSeats().stream()
                .map(ts -> ts.getSeat().getSeatNumber())
                .collect(Collectors.joining(", "));
    }
}
