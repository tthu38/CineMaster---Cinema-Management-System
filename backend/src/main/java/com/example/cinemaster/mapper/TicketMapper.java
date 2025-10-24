package com.example.cinemaster.mapper;

import com.example.cinemaster.dto.response.TicketDetailResponse;
import com.example.cinemaster.dto.response.TicketResponse;
import com.example.cinemaster.entity.Ticket;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.util.stream.Collectors;
import java.util.List;

@Mapper(componentModel = "spring", imports = {java.util.stream.Collectors.class, java.math.BigDecimal.class})
public interface TicketMapper {

    TicketMapper INSTANCE = Mappers.getMapper(TicketMapper.class);

    // ========================== FULL TICKET MAPPING ==========================
    @Mapping(target = "accountId", source = "account.accountID")
    @Mapping(target = "showtimeId", source = "showtime.showtimeID")
    @Mapping(target = "ticketStatus",
            expression = "java(ticket.getTicketStatus() != null ? ticket.getTicketStatus().name() : null)")
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
    @Mapping(target = "startTime", expression = "java(ticket.getShowtime().getStartTime())")
    @Mapping(target = "seatIds",
            expression = "java(ticket.getTicketSeats().stream()" +
                    ".map(ts -> ts.getSeat().getSeatID()).collect(Collectors.toList()))")
    @Mapping(target = "combos",
            expression = "java(ticket.getTicketCombos().stream()" +
                    ".map(tc -> new TicketResponse.ComboResponse(" +
                    "tc.getCombo().getId(), tc.getCombo().getNameCombo(), tc.getQuantity(), tc.getCombo().getPrice()))" +
                    ".collect(Collectors.toList()))")
    @Mapping(target = "discounts",
            expression = "java(ticket.getTicketDiscounts().stream()" +
                    ".map(td -> new TicketResponse.DiscountResponse(" +
                    "td.getDiscount().getDiscountID(), td.getDiscount().getCode(), td.getAmount()))" +
                    ".collect(Collectors.toList()))")
    @Mapping(target = "seatTotal",
            expression = "java(ticket.getTicketSeats().stream()" +
                    ".map(ts -> ts.getSeat().getSeatType().getPriceMultiplier().multiply(ticket.getShowtime().getPrice()))" +
                    ".reduce(BigDecimal.ZERO, BigDecimal::add))")
    @Mapping(target = "comboTotal",
            expression = "java(ticket.getTicketCombos().stream()" +
                    ".map(tc -> tc.getCombo().getPrice().multiply(BigDecimal.valueOf(tc.getQuantity())))" +
                    ".reduce(BigDecimal.ZERO, BigDecimal::add))")
    @Mapping(target = "originalPrice",
            expression = "java(ticket.getTicketSeats().stream()" +
                    ".map(ts -> ts.getSeat().getSeatType().getPriceMultiplier().multiply(ticket.getShowtime().getPrice()))" +
                    ".reduce(BigDecimal.ZERO, BigDecimal::add)" +
                    ".add(ticket.getTicketCombos().stream()" +
                    ".map(tc -> tc.getCombo().getPrice().multiply(BigDecimal.valueOf(tc.getQuantity())))" +
                    ".reduce(BigDecimal.ZERO, BigDecimal::add)))")
    @Mapping(target = "discountTotal",
            expression = "java(ticket.getTicketDiscounts().stream()" +
                    ".map(td -> td.getAmount()).reduce(BigDecimal.ZERO, BigDecimal::add))")
    @Mapping(target = "totalPrice", source = "totalPrice")
    @Mapping(target = "branchName",
            expression = "java(ticket.getShowtime().getAuditorium().getBranch().getBranchName())")
    // ✅ Thêm customerName để hiển thị tên người đặt
    @Mapping(target = "customerName",
            expression = "java(ticket.getAccount() != null ? ticket.getAccount().getFullName() : null)")
    TicketResponse toResponse(Ticket ticket);

    // ========================== LIST VIEW ==========================
    @Mapping(target = "ticketId", source = "ticketId")
    @Mapping(target = "movieTitle", source = "showtime.period.movie.title")
    @Mapping(target = "showtimeStart", source = "showtime.startTime")
    @Mapping(target = "branchName", source = "showtime.auditorium.branch.branchName")
    // ✅ Thêm mapping để danh sách vé hiển thị đúng tên khách hàng
    @Mapping(target = "customerName",
            expression = "java(entity.getAccount() != null ? entity.getAccount().getFullName() : \"Khách vãng lai\")")
    @Mapping(target = "seatNumbers", expression = "java(getSeatNumbers(entity))")
    @Mapping(target = "ticketStatus",
            expression = "java(entity.getTicketStatus() != null ? entity.getTicketStatus().name() : null)")
    TicketResponse toShortResponse(Ticket entity);

    // ========================== DETAIL VIEW ==========================
    @Mapping(target = "ticketId", source = "ticketId")
    @Mapping(target = "movieTitle", source = "showtime.period.movie.title")
    @Mapping(target = "movieGenre", source = "showtime.period.movie.genre")
    @Mapping(target = "movieDuration", source = "showtime.period.movie.duration")
    @Mapping(target = "showtimeStart", source = "showtime.startTime")
    @Mapping(target = "showtimeEnd", source = "showtime.endTime")
    @Mapping(target = "branchName", source = "showtime.auditorium.branch.branchName")
    @Mapping(target = "auditoriumName", source = "showtime.auditorium.name")
    @Mapping(target = "seatNumbers", expression = "java(getSeatNumbers(entity))")
    @Mapping(target = "ticketStatus",
            expression = "java(entity.getTicketStatus() != null ? entity.getTicketStatus().name() : null)")
    @Mapping(target = "totalPrice", source = "totalPrice")
    @Mapping(target = "paymentMethod",
            expression = "java(entity.getPaymentMethod() != null ? entity.getPaymentMethod().name() : null)")
    @Mapping(target = "comboList", ignore = true)
    // ✅ Gắn thêm customerName cho chi tiết vé
    @Mapping(target = "customerName",
            expression = "java(entity.getAccount() != null ? entity.getAccount().getFullName() : null)")
    TicketDetailResponse toDetailResponse(Ticket entity);

    // ========================== HELPER ==========================
    default String getSeatNumbers(Ticket ticket) {
        if (ticket.getTicketSeats() == null || ticket.getTicketSeats().isEmpty()) return "-";
        return ticket.getTicketSeats().stream()
                .map(ts -> ts.getSeat().getSeatRow() + ts.getSeat().getSeatNumber())
                .collect(Collectors.joining(", "));
    }
}
