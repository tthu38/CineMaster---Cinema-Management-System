package com.example.cinemaster.service;

import com.example.cinemaster.dto.request.OtpCheckRequest;
import com.example.cinemaster.dto.response.OtpCheckResponse;
import com.example.cinemaster.entity.*;
import com.example.cinemaster.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OtpCheckService {
    private final OtpRepository otpRepository;
    private final TicketComboRepository ticketComboRepository;

    public OtpCheckResponse checkOtp(OtpCheckRequest req) {
        var otp = otpRepository.findValidOtp(req.getCode(), LocalDateTime.now())
                .orElseThrow(() -> new IllegalArgumentException("❌ OTP không hợp lệ hoặc đã hết hạn!"));

        var ticket = otp.getTicket();
        if (ticket == null)
            throw new IllegalArgumentException("❌ OTP này chưa liên kết với vé nào!");

        var show = ticket.getShowtime();
        if (show == null)
            throw new IllegalArgumentException("❌ Vé này không có thông tin suất chiếu!");

        var period = show.getPeriod();
        var movie = (period != null) ? period.getMovie() : null;
        var auditorium = show.getAuditorium();
        var branch = (auditorium != null) ? auditorium.getBranch() : null;

        // Lấy combo
        var combos = ticketComboRepository.findByTicket_TicketId(ticket.getTicketId()).stream()
                .map(this::formatCombo)
                .collect(Collectors.toList());

        return OtpCheckResponse.builder()
                .movieTitle(movie.getTitle())
                .branchName(branch.getBranchName())
                .auditoriumName(auditorium.getName())
                .language(show.getLanguage())
                .startTime(show.getStartTime())
                .endTime(show.getEndTime())
                .seats(ticket.getTicketSeats().stream()
                        .map(ts -> ts.getSeat().getSeatNumber())
                        .collect(Collectors.toList()))
                .combos(combos)
                .totalPrice(ticket.getTotalPrice())
                .paymentMethod(ticket.getPaymentMethod() != null ? ticket.getPaymentMethod().name() : "UNKNOWN")  // ✅ fix dòng này
                .ticketStatus(ticket.getTicketStatus().name())
                .build();

    }


    private String formatCombo(TicketCombo tc) {
        var name = tc.getCombo() != null ? tc.getCombo().getNameCombo() : "Combo";
        var qty = tc.getQuantity() == null ? 1 : tc.getQuantity();
        return name + " (x" + qty + ")";
    }
}
