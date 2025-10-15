package com.example.cinemaster.mapper;

import com.example.cinemaster.dto.response.TicketResponse;
import com.example.cinemaster.entity.Ticket;
import org.mapstruct.*;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface TicketMapper {

    @Mapping(target = "ticketId", source = "ticketID")
    @Mapping(target = "movieTitle", source = "showtime.movie.movieTitle")
    @Mapping(target = "branchName", source = "showtime.auditorium.branch.branchName")
    @Mapping(target = "auditoriumName", source = "showtime.auditorium.auditoriumName")
    @Mapping(target = "showDate", expression = "java(ticket.getShowtime().getStartTime().toLocalDate().toString())")
    @Mapping(target = "showTime", expression = "java(ticket.getShowtime().getStartTime().toLocalTime().toString())")
    @Mapping(target = "seats", expression = "java(ticket.getTicketSeats().stream().map(ts -> ts.getSeat().getSeatNumber()).collect(Collectors.toList()))")
    @Mapping(target = "comboName", source = "combo.comboName")
    @Mapping(target = "comboPrice", source = "combo.price")
    @Mapping(target = "discountCode", expression = "java(ticket.getTicketDiscounts() != null && !ticket.getTicketDiscounts().isEmpty() ? ticket.getTicketDiscounts().get(0).getDiscountID().getDiscountCode() : null)")
    @Mapping(target = "discountAmount", expression = "java(ticket.getTicketDiscounts() != null && !ticket.getTicketDiscounts().isEmpty() ? ticket.getTicketDiscounts().get(0).getAmount() : null)")
    TicketResponse toResponse(Ticket ticket);
}
