package com.example.cinemaster.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TicketCreateRequest {

    Integer ticketId;
    Integer accountId;
    Integer showtimeId;
    List<Integer> seatIds;
    List<Integer> discountIds;
    List<ComboItem> combos;
    private String customerEmail;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ComboItem {
        Integer comboId;
        Integer quantity;
    }
}
