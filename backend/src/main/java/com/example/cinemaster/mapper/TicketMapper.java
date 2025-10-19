package com.example.cinemaster.mapper;

import com.example.cinemaster.dto.response.TicketDetailResponse;
import com.example.cinemaster.dto.response.TicketResponse;
import com.example.cinemaster.entity.Ticket;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import java.util.stream.Collectors;
import java.math.BigDecimal;

@Mapper(componentModel = "spring", imports = { java.util.stream.Collectors.class, java.math.BigDecimal.class })
public interface TicketMapper {

    TicketMapper INSTANCE = Mappers.getMapper(TicketMapper.class);

    // ========================== FULL TICKET MAPPING (gốc) ==========================
    @Mapping(target = "accountId", source = "account.accountID")
    @Mapping(target = "showtimeId", source = "showtime.showtimeID")
    @Mapping(target = "status", source = "ticketStatus")

    // 🎬 Thông tin phim, suất chiếu, rạp
    @Mapping(target = "movieTitle",
            expression = "java(ticket.getShowtime().getPeriod().getMovie().getTitle())")
    @Mapping(target = "auditoriumName",
            expression = "java(ticket.getShowtime().getAuditorium().getName())")
    @Mapping(target = "seatNames",
            expression = "java(ticket.getTicketSeats().stream()" +
                    ".map(ts -> ts.getSeat().getSeatRow() + ts.getSeat().getSeatNumber())" +
                    ".collect(Collectors.joining(\", \")))")
    @Mapping(target = "branchAddress",
            expression = "java(ticket.getShowtime().getAuditorium().getBranch().getAddress())")
    @Mapping(target = "startTime",
            expression = "java(ticket.getShowtime().getStartTime())")

    // 🎟️ Danh sách ID ghế
    @Mapping(target = "seatIds",
            expression = "java(ticket.getTicketSeats().stream()" +
                    ".map(ts -> ts.getSeat().getSeatID())" +
                    ".collect(Collectors.toList()))")

    // 🍿 Danh sách combo chi tiết
    @Mapping(target = "combos",
            expression = "java(ticket.getTicketCombos().stream()" +
                    ".map(tc -> new TicketResponse.ComboResponse(" +
                    "tc.getCombo().getId(), " +
                    "tc.getCombo().getNameCombo(), " +
                    "tc.getQuantity(), " +
                    "tc.getCombo().getPrice()))" +
                    ".collect(Collectors.toList()))")

    // 🎁 Danh sách giảm giá
    @Mapping(target = "discounts",
            expression = "java(ticket.getTicketDiscounts().stream()" +
                    ".map(td -> new TicketResponse.DiscountResponse(" +
                    "td.getDiscount().getDiscountID(), " +
                    "td.getDiscount().getCode(), " +
                    "td.getAmount()))" +
                    ".collect(Collectors.toList()))")

    // 💺 Tổng tiền ghế
    @Mapping(target = "seatTotal",
            expression = "java(ticket.getTicketSeats().stream()" +
                    ".map(ts -> ts.getSeat().getSeatType().getPriceMultiplier().multiply(ticket.getShowtime().getPrice()))" +
                    ".reduce(BigDecimal.ZERO, BigDecimal::add))")

    // 🍿 Tổng tiền combo
    @Mapping(target = "comboTotal",
            expression = "java(ticket.getTicketCombos().stream()" +
                    ".map(tc -> tc.getCombo().getPrice().multiply(BigDecimal.valueOf(tc.getQuantity())))" +
                    ".reduce(BigDecimal.ZERO, BigDecimal::add))")

    // 💸 Tổng tiền gốc (ghế + combo)
    @Mapping(target = "originalPrice",
            expression = "java(ticket.getTicketSeats().stream()" +
                    ".map(ts -> ts.getSeat().getSeatType().getPriceMultiplier().multiply(ticket.getShowtime().getPrice()))" +
                    ".reduce(BigDecimal.ZERO, BigDecimal::add)" +
                    ".add(ticket.getTicketCombos().stream()" +
                    ".map(tc -> tc.getCombo().getPrice().multiply(BigDecimal.valueOf(tc.getQuantity())))" +
                    ".reduce(BigDecimal.ZERO, BigDecimal::add)))")

    // 🔻 Tổng giảm giá
    @Mapping(target = "discountTotal",
            expression = "java(ticket.getTicketDiscounts().stream()" +
                    ".map(td -> td.getAmount())" +
                    ".reduce(BigDecimal.ZERO, BigDecimal::add))")

    // ✅ Tổng cuối (đã lưu DB)
    @Mapping(target = "totalPrice", source = "totalPrice")
    TicketResponse toResponse(Ticket ticket);

    // ========================== LIST VIEW (rút gọn) ==========================
    @Mapping(target = "ticketId", source = "ticketId")
    @Mapping(target = "movieTitle", source = "showtime.period.movie.title")
    @Mapping(target = "showtimeStart", source = "showtime.startTime")
    @Mapping(target = "branchName", source = "showtime.period.branch.branchName")
    @Mapping(target = "customerName", source = "account.fullName")
    @Mapping(target = "seatNumbers", expression = "java(getSeatNumbers(entity))")
    TicketResponse toShortResponse(Ticket entity);

    // ========================== CHI TIẾT VÉ (TicketDetailResponse) ==========================
    @Mapping(target = "ticketId", source = "ticketId")
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

    // ========================== HELPER ==========================
    default String getSeatNumbers(Ticket ticket) {
        if (ticket.getTicketSeats() == null || ticket.getTicketSeats().isEmpty()) return "-";
        return ticket.getTicketSeats().stream()
                .map(ts -> ts.getSeat().getSeatRow() + ts.getSeat().getSeatNumber())
                .collect(Collectors.joining(", "));
    }
}
