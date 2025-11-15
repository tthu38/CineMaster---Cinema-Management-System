package com.example.cinemaster.controller;

import com.example.cinemaster.dto.request.TicketPdfRequest;
import com.example.cinemaster.service.TicketPdfService;
import com.example.cinemaster.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ticket")
@RequiredArgsConstructor
public class TicketPdfController {

    private final TicketPdfService ticketPdfService;
    private final TicketService ticketService;  // ⭐ THÊM VÀO

    @PostMapping("/print")
    public ResponseEntity<byte[]> printTicket(@RequestBody TicketPdfRequest req) {
        try {

            // ⭐⭐ UPDATE TRẠNG THÁI VÉ TRƯỚC KHI IN PDF ⭐⭐
            if (req.getTicketId() != null) {
                ticketService.updateTicketStatus(req.getTicketId(), "USED", null);
            }

            // ⭐ Generate PDF
            byte[] pdf = ticketPdfService.generatePdf(req);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=ticket.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}