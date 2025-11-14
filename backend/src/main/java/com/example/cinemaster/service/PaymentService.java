package com.example.cinemaster.service;


import com.example.cinemaster.entity.Payment;
import com.example.cinemaster.entity.Ticket;
import com.example.cinemaster.entity.TicketHistory;
import com.example.cinemaster.repository.PaymentRepository;
import com.example.cinemaster.repository.TicketHistoryRepository;
import com.example.cinemaster.repository.TicketRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
public class PaymentService {


    private final TicketRepository ticketRepository;
    private final PaymentRepository paymentRepository;
    private final TicketHistoryRepository ticketHistoryRepository;


    /**  Ghi nhận thanh toán thành công & cập nhật vé */
    @Transactional
    public void confirmPaid(String orderCode, String note, BigDecimal amount, Integer ticketId) {
        // Tìm Payment theo orderCode
        Payment payment = paymentRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy payment " + orderCode));


        // Lấy Ticket liên quan
        Ticket ticket = payment.getTicketID();
        if (ticket == null) {
            throw new RuntimeException("Không tìm thấy vé liên quan đến thanh toán: " + orderCode);
        }


        // Cập nhật vé thành BOOKED
        Ticket.TicketStatus oldStatus = ticket.getTicketStatus();
        ticket.setTicketStatus(Ticket.TicketStatus.BOOKED);
        ticketRepository.save(ticket);


        // Ghi lịch sử vé ( đổi Instant → LocalDateTime)
        TicketHistory history = TicketHistory.builder()
                .ticket(ticket)
                .oldStatus(oldStatus != null ? oldStatus.name() : "NULL")
                .newStatus("BOOKED")
                .changedAt(LocalDateTime.now())
                .note(note != null ? note : "Thanh toán thành công qua QR")
                .build();
        ticketHistoryRepository.save(history);


        //  Cập nhật Payment
        payment.setStatus("PAID");
        payment.setDescription(note);
        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepository.save(payment);


        System.out.println(" Đã xác nhận thanh toán cho vé ID=" + ticket.getTicketId());
    }






}

