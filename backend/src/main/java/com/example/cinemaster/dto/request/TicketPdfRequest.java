package com.example.cinemaster.dto.request;


import lombok.Data;


import java.util.List;


@Data
public class TicketPdfRequest {
    private String movieTitle;
    private String branchName;
    private String auditoriumName;
    private String showDate;
    private String showTime;
    private String seat;
    private String price;
    private String paymentMethod;

    private String transactionId;
    private String customerName;
    private String transactionTime;

    private List<String> combos;

    private Integer ticketId;

}


