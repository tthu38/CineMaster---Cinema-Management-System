package com.example.cinemaster.service;


import com.example.cinemaster.dto.request.OtpCheckRequest;
import com.example.cinemaster.dto.response.OtpCheckResponse;
import com.example.cinemaster.entity.*;
import com.example.cinemaster.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class OtpCheckService {


    private final OtpRepository otpRepository;
    private final TicketComboRepository ticketComboRepository;
    private final TicketService ticketService;
    private final TicketRepository ticketRepository;


    public OtpCheckResponse checkOtp(OtpCheckRequest req) {


        var otp = otpRepository.findValidOtp(req.getCode(), LocalDateTime.now())
                .orElseThrow(() -> new IllegalArgumentException("❌ OTP không hợp lệ hoặc đã hết hạn!"));


        var ticket = otp.getTicket();

        // ========== Cập nhật trạng thái vé BOOKED → USED + ghi lịch sử ==========
        if (ticket.getTicketStatus() == Ticket.TicketStatus.BOOKED) {

            String oldStatus = ticket.getTicketStatus().name();

            ticket.setTicketStatus(Ticket.TicketStatus.USED);
            ticketRepository.save(ticket);

            ticketService.saveTicketHistory(
                    ticket,
                    oldStatus,
                    Ticket.TicketStatus.USED.name(),
                    otp.getAccountID(),                 // Nhân viên check-in
                    "Check-in OTP tại quầy"
            );
        }

        if (ticket == null)
            throw new IllegalArgumentException("❌ OTP này chưa liên kết với vé nào!");


        var show = ticket.getShowtime();
        if (show == null)
            throw new IllegalArgumentException("❌ Vé này không có thông tin suất chiếu!");


        var period = show.getPeriod();
        var movie = (period != null) ? period.getMovie() : null;
        var auditorium = show.getAuditorium();
        var branch = (auditorium != null) ? auditorium.getBranch() : null;


        // ✅ Lấy danh sách ghế (ghép Row + Number => A10, B3,...)
        var ticketSeats = ticket.getTicketSeats();
        List<String> seatCodes = ticketSeats.stream()
                .map(ts -> {
                    var s = ts.getSeat();
                    return (s.getSeatRow() != null ? s.getSeatRow() : "")
                            + (s.getSeatNumber() != null ? s.getSeatNumber() : "");
                })
                .collect(Collectors.toList());


        List<Integer> seatIds = ticketSeats.stream()
                .map(ts -> ts.getSeat().getSeatID())
                .collect(Collectors.toList());


        // ✅ Lấy danh sách combo (nếu có)
        var combos = ticketComboRepository.findByTicket_TicketId(ticket.getTicketId()).stream()
                .map(this::formatCombo)
                .collect(Collectors.toList());


        return OtpCheckResponse.builder()

                .ticketId(ticket.getTicketId())

                // ===== ID phục vụ FE tra cứu =====
                .showtimeId(show.getShowtimeID())
                .auditoriumId(auditorium != null ? auditorium.getAuditoriumID() : null)


                // ===== Thông tin vé =====
                .movieTitle(movie != null ? movie.getTitle() : "Không rõ")
                .branchName(branch != null ? branch.getBranchName() : "Không rõ")
                .auditoriumName(auditorium != null ? auditorium.getName() : "Không rõ")
                .language(show.getLanguage())
                .startTime(show.getStartTime())
                .endTime(show.getEndTime())


                // ===== Ghế & Combo =====
                .seats(seatCodes)     // ⚡ Giờ trả về ["A10", "B3", ...]
                .seatIds(seatIds)
                .combos(combos)


                // ===== Thanh toán =====
                .totalPrice(ticket.getTotalPrice())
                .paymentMethod(ticket.getPaymentMethod() != null ? ticket.getPaymentMethod().name() : "UNKNOWN")
                .ticketStatus(ticket.getTicketStatus().name())
                .build();
    }


    private String formatCombo(TicketCombo tc) {
        var name = tc.getCombo() != null ? tc.getCombo().getNameCombo() : "Combo";
        var qty = tc.getQuantity() == null ? 1 : tc.getQuantity();
        return name + " (x" + qty + ")";
    }
}
