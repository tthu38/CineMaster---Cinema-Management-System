package com.example.cinemaster.service;

import com.example.cinemaster.dto.request.ComboRequest;
import com.example.cinemaster.dto.request.TicketComboRequest;
import com.example.cinemaster.dto.request.TicketCreateRequest;
import com.example.cinemaster.dto.response.TicketDetailResponse;
import com.example.cinemaster.dto.response.TicketResponse;
import com.example.cinemaster.entity.*;
import com.example.cinemaster.mapper.TicketMapper;
import com.example.cinemaster.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;




import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketComboRepository ticketComboRepository;
    private final AccountRepository accountRepository;
    private final ShowtimeRepository showtimeRepository;
    private final SeatRepository seatRepository;
    private final DiscountRepository discountRepository;
    private final ComboRepository comboRepository;
    private final TicketMapper ticketMapper;
    private final EmailService emailService;
    private final MembershipService membershipService;
    private final OtpRepository otpRepository;
    private final TicketHistoryRepository ticketHistoryRepository;
    private final GoogleSheetsService googleSheetsService;



    /* 🟢 Tạo hoặc cập nhật vé tạm */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TicketResponse createOrUpdateTicket(TicketCreateRequest req) {
        Account account = accountRepository.findById(req.getAccountId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản!"));
        Showtime showtime = showtimeRepository.findById(req.getShowtimeId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy suất chiếu!"));

        Ticket ticket;
        Integer ticketId = (req.getTicketId() != null && req.getTicketId() > 0) ? req.getTicketId() : null;

        // ======================= 🧩 1. Tạo hoặc tải vé tạm =======================
        if (ticketId != null) {
            ticket = ticketRepository.findById(ticketId).orElse(null);

            if (ticket == null
                    || ticket.getAccount() == null
                    || !ticket.getAccount().getAccountID().equals(req.getAccountId())
                    || ticket.getTicketStatus() != Ticket.TicketStatus.HOLDING
                    || (ticket.getHoldUntil() != null && ticket.getHoldUntil().isBefore(LocalDateTime.now()))) {

                ticket = Ticket.builder()
                        .account(account)
                        .showtime(showtime)
                        .ticketStatus(Ticket.TicketStatus.HOLDING)
                        .paymentMethod(Ticket.PaymentMethod.CASH)
                        .holdUntil(LocalDateTime.now().plusMinutes(5))
                        .ticketSeats(new ArrayList<>())
                        .ticketCombos(new ArrayList<>())
                        .ticketDiscounts(new ArrayList<>())
                        .build();
                ticketRepository.save(ticket);
            } else {
                ticket.getTicketSeats().clear();
                ticket.getTicketDiscounts().clear();
                ticket.setHoldUntil(LocalDateTime.now().plusMinutes(5));
                ticketRepository.save(ticket);
            }
        } else {
            List<Ticket> oldHolding = ticketRepository
                    .findByAccount_AccountIDAndTicketStatus(req.getAccountId(), Ticket.TicketStatus.HOLDING);

            for (Ticket old : oldHolding) {
                old.getTicketSeats().clear();
                old.getTicketCombos().clear();
                old.getTicketDiscounts().clear();
                old.setTicketStatus(Ticket.TicketStatus.CANCELLED);
                old.setHoldUntil(null);
            }
            if (!oldHolding.isEmpty()) {
                ticketRepository.saveAll(oldHolding);
                ticketRepository.flush();
            }

            ticket = Ticket.builder()
                    .account(account)
                    .showtime(showtime)
                    .ticketStatus(Ticket.TicketStatus.HOLDING)
                    .paymentMethod(Ticket.PaymentMethod.CASH)
                    .holdUntil(LocalDateTime.now().plusMinutes(5))
                    .ticketSeats(new ArrayList<>())
                    .ticketCombos(new ArrayList<>())
                    .ticketDiscounts(new ArrayList<>())
                    .build();
            ticketRepository.save(ticket);
        }

        // ======================= 🎟️ 2. Xử lý GHẾ =======================
        List<Integer> occupiedSeats = ticketRepository.findOccupiedSeatIdsByShowtime(showtime.getShowtimeID());
        BigDecimal seatTotal = BigDecimal.ZERO;

        if (req.getSeatIds() != null) {
            for (Integer seatId : req.getSeatIds()) {
                if (occupiedSeats.contains(seatId))
                    throw new RuntimeException("Ghế " + seatId + " đã được giữ hoặc đặt!");

                Seat seat = seatRepository.findById(seatId)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy ghế ID=" + seatId));
                BigDecimal seatPrice = showtime.getPrice().multiply(seat.getSeatType().getPriceMultiplier());
                seatTotal = seatTotal.add(seatPrice);
                ticket.getTicketSeats().add(new TicketSeat(ticket, seat));
            }
        }

        // ======================= 🍿 3. Tính COMBO (chưa lưu) =======================
        BigDecimal comboTotal = BigDecimal.ZERO;
        if (req.getCombos() != null && !req.getCombos().isEmpty()) {
            for (var comboReq : req.getCombos()) {
                Combo combo = comboRepository.findById(comboReq.getComboId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy combo!"));
                BigDecimal comboSubtotal = combo.getPrice()
                        .multiply(BigDecimal.valueOf(comboReq.getQuantity()));
                comboTotal = comboTotal.add(comboSubtotal);
            }
            log.info("🍿 Đang chọn {} combo (tạm tính: {}₫)", req.getCombos().size(), comboTotal);
        }

        // ======================= 🎁 4. Áp dụng DISCOUNT (nếu có) =======================
        BigDecimal discountTotal = BigDecimal.ZERO;
        if (req.getDiscountIds() != null) {
            for (Integer discountId : req.getDiscountIds()) {
                Discount discount = discountRepository.findById(discountId)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy mã giảm giá!"));
                BigDecimal discountValue = discount.getValue(seatTotal.add(comboTotal));
                discountTotal = discountTotal.add(discountValue);

                TicketDiscount td = TicketDiscount.builder()
                        .ticket(ticket)
                        .discount(discount)
                        .discountId(discountId)
                        .amount(discountValue)
                        .build();
                ticket.getTicketDiscounts().add(td);
            }
        }

        // ======================= 💰 5. Cập nhật tổng tiền tạm tính =======================
        BigDecimal total = seatTotal.add(comboTotal).subtract(discountTotal);
        if (total.compareTo(BigDecimal.ZERO) < 0) total = BigDecimal.ZERO;
        ticket.setTotalPrice(total);

        ticketRepository.save(ticket);

        log.info("💰 VÉ {}: Seat={} | Combo(tạm)={} | Discount={} | Final={}",
                ticket.getTicketId(), seatTotal, comboTotal, discountTotal, total);

        ticketRepository.flush();
        return ticketMapper.toResponse(ticket);
    }

    /* 🟢 Đổi ghế khi quay lại */
    @Transactional
    public TicketResponse replaceSeats(Integer ticketId, List<Integer> newSeatIds) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy vé!"));
        if (ticket.getTicketStatus() != Ticket.TicketStatus.HOLDING)
            throw new RuntimeException("Chỉ đổi ghế khi vé đang HOLDING!");

        ticket.getTicketSeats().clear();
        List<Integer> occupied = ticketRepository.findOccupiedSeatIdsByShowtime(ticket.getShowtime().getShowtimeID());
        BigDecimal total = BigDecimal.ZERO;

        for (Integer seatId : newSeatIds) {
            if (occupied.contains(seatId))
                throw new RuntimeException("Ghế " + seatId + " đã có người giữ!");
            Seat seat = seatRepository.findById(seatId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy ghế!"));
            BigDecimal price = ticket.getShowtime().getPrice().multiply(seat.getSeatType().getPriceMultiplier());
            total = total.add(price);
            ticket.getTicketSeats().add(new TicketSeat(ticket, seat));
        }

        ticket.setTotalPrice(total);
        ticket.setHoldUntil(LocalDateTime.now().plusMinutes(5));
        ticketRepository.save(ticket);
        return ticketMapper.toResponse(ticket);
    }

    // 🟢 Sinh OTP cho mỗi vé (hóa đơn)
    private String createOtpForTicket(Ticket ticket) {
        java.security.SecureRandom random = new java.security.SecureRandom();
        int otpValue = 100000 + random.nextInt(900000); // Random 6 chữ số

        LocalDateTime expiryTime = ticket.getShowtime().getEndTime();
        if (expiryTime == null)
            expiryTime = ticket.getShowtime().getStartTime().plusHours(2);

        Otp otp = Otp.builder()
                .accountID(ticket.getAccount())
                .ticket(ticket)
                .code(String.valueOf(otpValue))
                .expiry(expiryTime)
                .build();

        otpRepository.save(otp);
        log.info("🔑 Đã tạo OTP {} cho vé {} (hết hạn {})", otpValue, ticket.getTicketId(), expiryTime);

        return String.valueOf(otpValue); // ✅ trả lại code để gửi mail
    }


//    @Transactional
//    public void confirmPayment(Integer ticketId, List<TicketComboRequest> combos, String customEmail) {
//        Ticket ticket = ticketRepository.findById(ticketId)
//                .orElseThrow(() -> new RuntimeException("Không tìm thấy vé!"));
//
//        if (ticket.getTicketStatus() != Ticket.TicketStatus.HOLDING)
//            throw new RuntimeException("Vé không ở trạng thái HOLDING!");
//
//        // 🧩 Ép Hibernate load danh sách liên quan
//        ticket.getTicketSeats().size();
//        ticket.getTicketDiscounts().size();
//        ticket.getTicketCombos().size(); //
//        Showtime showtime = ticket.getShowtime();
//        BigDecimal seatTotal = ticket.getTicketSeats().stream()
//                .map(ts -> showtime.getPrice().multiply(ts.getSeat().getSeatType().getPriceMultiplier()))
//                .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//        // ====================== 🍿 1️⃣ TẠO COMBO CHÍNH THỨC ======================
//        BigDecimal comboTotal = BigDecimal.ZERO;
//
//        List<TicketComboRequest> finalCombos;
//
//// ✅ Nếu combos null (ví dụ từ polling) → lấy từ DB
//        if (combos == null || combos.isEmpty()) {
//            finalCombos = ticket.getTicketCombos().stream()
//                    .map(tc -> new TicketComboRequest(tc.getCombo().getId(), tc.getQuantity()))
//                    .collect(Collectors.toList());
//        } else {
//            finalCombos = combos;
//        }
//
//// 🔹 Xóa combo cũ (nếu có)
//        List<TicketCombo> oldCombos = ticketComboRepository.findByTicket_TicketId(ticketId);
//        if (!oldCombos.isEmpty()) {
//            ticketComboRepository.deleteAll(oldCombos);
//            log.info("🧹 Đã xóa combo cũ của vé {}", ticketId);
//        }
//
//// 🔹 Tạo combo mới
//        for (TicketComboRequest comboReq : finalCombos) {
//            Combo combo = comboRepository.findById(comboReq.getComboId())
//                    .orElseThrow(() -> new RuntimeException("Không tìm thấy combo!"));
//
//            BigDecimal subtotal = combo.getPrice()
//                    .multiply(BigDecimal.valueOf(comboReq.getQuantity()));
//            comboTotal = comboTotal.add(subtotal);
//
//            TicketCombo ticketCombo = TicketCombo.builder()
//                    .ticket(ticket)
//                    .combo(combo)
//                    .ticketId(ticket.getTicketId())
//                    .comboId(combo.getId())
//                    .quantity(comboReq.getQuantity())
//                    .build();
//
//            ticketComboRepository.save(ticketCombo);
//        }
//
//
//        // ====================== 🎁 2️⃣ TÍNH LẠI GIẢM GIÁ ======================
//        BigDecimal discountTotal = ticket.getTicketDiscounts().stream()
//                .map(TicketDiscount::getAmount)
//                .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//        // ====================== 💰 3️⃣ TÍNH LẠI TỔNG TIỀN ======================
//        BigDecimal total = seatTotal.add(comboTotal).subtract(discountTotal);
//        if (total.compareTo(BigDecimal.ZERO) < 0) total = BigDecimal.ZERO;
//
//        // 💾 Cập nhật vé
//        ticket.setTotalPrice(total);
//        ticket.setTicketStatus(Ticket.TicketStatus.BOOKED);
//        ticket.setHoldUntil(null);
//        ticketRepository.save(ticket);
//
//        // ====================== 🔐 4️⃣ TẠO OTP & GỬI MAIL ======================
//        String otpCode = createOtpForTicket(ticket);
//
//        try {
//            Account account = ticket.getAccount();
//            Movie movie = showtime != null ? showtime.getPeriod().getMovie() : null;
//            Auditorium auditorium = showtime != null ? showtime.getAuditorium() : null;
//            Branch branch = (auditorium != null) ? auditorium.getBranch() : null;
//
//            String recipient = (customEmail != null && !customEmail.isBlank())
//                    ? customEmail
//                    : (account != null ? account.getEmail() : ticket.getCustomerEmail());
//
//            if (recipient == null || recipient.isBlank()) {
//                log.warn("⚠️ Vé {} không có email hợp lệ, bỏ qua gửi mail.", ticket.getTicketId());
//            } else {
//                emailService.sendBookingConfirmationEmail(
//                        recipient,
//                        "CM-" + ticket.getTicketId(),
//                        movie != null ? movie.getTitle() : "Không xác định",
//                        auditorium != null ? auditorium.getName() : "Không xác định",
//                        ticket.getTicketSeats().stream()
//                                .map(ts -> ts.getSeat().getSeatRow() + ts.getSeat().getSeatNumber())
//                                .collect(Collectors.joining(", ")),
//                        showtime != null ? showtime.getStartTime() : LocalDateTime.now(),
//                        comboTotal != null ? comboTotal : BigDecimal.ZERO,
//                        seatTotal.add(comboTotal != null ? comboTotal : BigDecimal.ZERO),
//                        discountTotal != null ? discountTotal : BigDecimal.ZERO,
//                        ticket.getTotalPrice() != null ? ticket.getTotalPrice() : BigDecimal.ZERO,
//                        (branch != null && branch.getAddress() != null) ? branch.getAddress() : "Không xác định",
//                        "https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=ticket-" + ticket.getTicketId(),
//                        otpCode,
//                        ticketComboRepository.findByTicket_TicketId(ticketId).stream()
//                                .map(tc -> String.format("%dx %s (%,.0f VND)",
//                                        tc.getQuantity(),
//                                        tc.getCombo().getNameCombo(),
//                                        tc.getCombo().getPrice().multiply(BigDecimal.valueOf(tc.getQuantity()))))
//                                .collect(Collectors.toList())
//                );
//                log.info("📧 Đã gửi mail xác nhận vé cho {}", recipient);
//            }
//        } catch (Exception e) {
//            log.error("⚠️ Gửi mail thất bại nhưng KHÔNG rollback vé: {}", e.getMessage(), e);
//        }
//
//
//
//
//    }

    @Transactional
    public void cancelTicket(Integer ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy vé!"));
        if (ticket.getTicketStatus() == Ticket.TicketStatus.HOLDING)
            ticket.setTicketStatus(Ticket.TicketStatus.CANCELLED);
        else if (ticket.getTicketStatus() == Ticket.TicketStatus.BOOKED)
            ticket.setTicketStatus(Ticket.TicketStatus.CANCEL_REQUESTED);
        ticket.setHoldUntil(null);
        ticketRepository.save(ticket);
    }

    public TicketResponse getTicket(Integer id) {
        return ticketRepository.findById(id)
                .map(ticketMapper::toResponse)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy vé!"));
    }

    public List<Integer> getHeldSeats(Integer ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy vé!"));
        if (ticket.getTicketStatus() != Ticket.TicketStatus.HOLDING)
            throw new RuntimeException("Vé không ở trạng thái HOLDING!");
        return ticket.getTicketSeats().stream()
                .map(ts -> ts.getSeat().getSeatID())
                .toList();
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void releaseExpiredHolds() {
        List<Ticket> expired = ticketRepository
                .findByTicketStatusAndHoldUntilBefore(Ticket.TicketStatus.HOLDING, LocalDateTime.now());
        if (!expired.isEmpty()) {
            expired.forEach(t -> {
                t.setTicketStatus(Ticket.TicketStatus.CANCELLED);
                t.setHoldUntil(null);
            });
            ticketRepository.saveAll(expired);
        }
    }

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void addCombosToTicket(Integer ticketId, List<TicketComboRequest> combos) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy vé!"));

        List<TicketCombo> oldCombos = ticketComboRepository.findByTicket_TicketId(ticketId);
        if (!oldCombos.isEmpty()) {
            ticketComboRepository.deleteAll(oldCombos);
            if (!oldCombos.isEmpty()) {
                ticketComboRepository.deleteAll(oldCombos);
                ticketComboRepository.flush(); // 💥 đảm bảo DELETE thực thi ngay
                entityManager.detach(ticket); // 💥 clear cache
                log.info("🧹 Đã xóa combo cũ của vé {}", ticketId);
            }

            ticket.getTicketCombos().clear();
            ticketRepository.flush();

            entityManager.detach(ticket);
            ticket = ticketRepository.findById(ticketId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy vé!"));
            log.info("🧹 Đã xóa combo cũ của vé {}", ticketId);
        }

        BigDecimal comboTotal = BigDecimal.ZERO;
        for (var comboReq : combos) {
            Combo combo = comboRepository.findById(comboReq.getComboId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy combo!"));
            BigDecimal subtotal = combo.getPrice()
                    .multiply(BigDecimal.valueOf(comboReq.getQuantity()));
            comboTotal = comboTotal.add(subtotal);

            TicketCombo tc = TicketCombo.builder()
                    .ticket(ticket)
                    .combo(combo)
                    .ticketId(ticket.getTicketId())
                    .comboId(combo.getId())
                    .quantity(comboReq.getQuantity())
                    .build();
            ticketComboRepository.save(tc);
        }

        // ❌ Bỏ dòng ticket.setComboPrice(comboTotal);
        // ✅ chỉ lưu totalPrice khi cần thiết
        ticketRepository.saveAndFlush(ticket);

        log.info("🍿 Vé {} đã thêm {} combo mới (tổng {}₫)", ticketId, combos.size(), comboTotal);
    }


    private void saveTicketHistory(Ticket ticket, String oldStatus, String newStatus, Account changer, String note) {
        // ⚡ Nếu changer chỉ có ID (transient) => fetch entity thật
        Account realChanger = null;
        if (changer != null && changer.getAccountID() != null) {
            realChanger = accountRepository.findById(changer.getAccountID()).orElse(null);
        }

        TicketHistory history = TicketHistory.builder()
                .ticket(ticket)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .changedBy(realChanger)
                .changedAt(LocalDateTime.now())
                .note(note)
                .build();

        ticketHistoryRepository.save(history);
    }

    @Transactional
    public TicketResponse requestCancel(Integer ticketId, Account requester) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy vé"));

        Ticket.TicketStatus current = ticket.getTicketStatus();
        if (current != Ticket.TicketStatus.BOOKED)
            throw new RuntimeException("Chỉ có thể gửi yêu cầu hủy khi vé đang ở trạng thái 'BOOKED'");

        ticket.setTicketStatus(Ticket.TicketStatus.CANCEL_REQUESTED);
        ticketRepository.save(ticket);

        Account realCustomer = ticket.getAccount();

        saveTicketHistory(ticket,
                current.name(),
                Ticket.TicketStatus.CANCEL_REQUESTED.name(),
                realCustomer,
                "Khách hàng yêu cầu hủy vé");

        return ticketMapper.toResponse(ticket);
    }

    @Transactional
    public TicketResponse approveCancel(Integer ticketId, Account staff) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy vé!"));

        Ticket.TicketStatus currentStatus = ticket.getTicketStatus();
        if (currentStatus != Ticket.TicketStatus.CANCEL_REQUESTED)
            throw new RuntimeException("Chỉ phê duyệt khi vé ở trạng thái 'CANCEL_REQUESTED'");

        // 🔹 Lưu trạng thái cũ và cập nhật mới
        String oldStatus = currentStatus.name();
        ticket.setTicketStatus(Ticket.TicketStatus.CANCELLED);
        ticketRepository.save(ticket);

        // 🔹 Ghi lại lịch sử thay đổi
        saveTicketHistory(ticket, oldStatus, Ticket.TicketStatus.CANCELLED.name(), staff, "Nhân viên duyệt hủy vé");

        log.info("✅ Vé {} được staff {} duyệt hủy thành công.", ticketId,
                staff != null ? staff.getFullName() : "Unknown");

        return ticketMapper.toResponse(ticket);
    }

    @Transactional
    public TicketResponse approveRefund(Integer ticketId, Account staff) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy vé!"));

        Ticket.TicketStatus currentStatus = ticket.getTicketStatus();
        if (currentStatus != Ticket.TicketStatus.CANCELLED)
            throw new RuntimeException("Chỉ hoàn tiền cho vé đã bị hủy (CANCELLED)!");

        // 🔹 Lưu lại trạng thái cũ và cập nhật mới
        String oldStatus = currentStatus.name();
        ticket.setTicketStatus(Ticket.TicketStatus.REFUNDED);
        ticketRepository.save(ticket);

        // 🔹 Ghi lịch sử
        saveTicketHistory(ticket, oldStatus, Ticket.TicketStatus.REFUNDED.name(), staff, "Nhân viên xác nhận hoàn tiền vé");

        log.info("💸 Vé {} đã được hoàn tiền bởi nhân viên {}.", ticketId,
                staff != null ? staff.getFullName() : "Unknown");

        return ticketMapper.toResponse(ticket);
    }

    public List<TicketResponse> getPendingCancelTickets(Integer branchId) {
        return ticketRepository.findByBranch(branchId).stream()
                .filter(t -> t.getTicketStatus() == Ticket.TicketStatus.CANCEL_REQUESTED)
                .map(ticketMapper::toResponse)
                .collect(Collectors.toList());
    }

    //CUSTOMER: Lấy danh sách vé của người dùng
    public List<TicketResponse> getTicketsByAccount(Integer accountID) {
        return ticketRepository.findByAccount_AccountID(accountID).stream()
                .map(ticketMapper::toResponse)
                .collect(Collectors.toList());
    }

    // STAFF: Lấy danh sách vé theo chi nhánh
    public List<TicketResponse> getTicketsByBranch(Integer branchId) {
        return ticketRepository.findByBranch(branchId).stream()
                .map(ticketMapper::toResponse)
                .collect(Collectors.toList());
    }

    //       STAFF: Cập nhật trạng thái thủ công (debug / special case)
    @Transactional
    public TicketResponse updateTicketStatus(Integer ticketId, String newStatus, Account staff) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy vé!"));

        Ticket.TicketStatus oldStatus = ticket.getTicketStatus();

        // 🔹 Kiểm tra hợp lệ của trạng thái mới
        Ticket.TicketStatus newEnumStatus;
        try {
            newEnumStatus = Ticket.TicketStatus.valueOf(newStatus.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("❌ Trạng thái không hợp lệ: " + newStatus);
        }

        // 🔹 Cập nhật và lưu vé
        ticket.setTicketStatus(newEnumStatus);
        ticketRepository.save(ticket);

        // 🔹 Lưu lịch sử thay đổi
        saveTicketHistory(ticket, oldStatus.name(), newEnumStatus.name(), staff, "Cập nhật trạng thái thủ công");

        log.info("🛠️ Vé {} được cập nhật từ {} ➜ {} bởi nhân viên {}",
                ticketId, oldStatus, newEnumStatus,
                staff != null ? staff.getFullName() : "Unknown");

        return ticketMapper.toResponse(ticket);
    }

    public TicketDetailResponse getById(Integer id) {
        Ticket ticket = ticketRepository.findWithRelationsByTicketId(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy vé!"));

        TicketDetailResponse dto = ticketMapper.toDetailResponse(ticket);

        // ================= 🎟️ Ghế =================
        String seatNums = (ticket.getTicketSeats() != null && !ticket.getTicketSeats().isEmpty())
                ? ticket.getTicketSeats().stream()
                .map(ts -> ts.getSeat().getSeatRow() + ts.getSeat().getSeatNumber())
                .collect(Collectors.joining(", "))
                : "N/A";
        dto.setSeatNumbers(seatNums);

        // ================= 🍿 Combo =================
        List<String> comboList = (ticket.getTicketCombos() != null && !ticket.getTicketCombos().isEmpty())
                ? ticket.getTicketCombos().stream()
                .map(tc -> String.format("%s x%d",
                        tc.getCombo().getNameCombo(),
                        tc.getQuantity() != null ? tc.getQuantity() : 1))
                .collect(Collectors.toList())
                : List.of();
        dto.setComboList(comboList);

        // ================= ⚙️ Thông tin bổ sung =================
        dto.setTicketStatus(ticket.getTicketStatus().name()); // Enum ➜ String
        dto.setTotalPrice(ticket.getTotalPrice() != null
                ? ticket.getTotalPrice().doubleValue()
                : 0.0);
        dto.setPaymentMethod(ticket.getPaymentMethod() != null
                ? ticket.getPaymentMethod().toString()
                : "UNKNOWN");

        return dto;
    }


    @Transactional
    public TicketResponse verifyOnlinePayment(Integer ticketId) throws Exception {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy vé!"));
        if (ticket.getTicketStatus() != Ticket.TicketStatus.HOLDING)
            throw new RuntimeException("Chỉ kiểm tra thanh toán cho vé HOLDING!");

        // ✅ Kiểm tra phương thức
        if (ticket.getPaymentMethod() != Ticket.PaymentMethod.ONLINE)
            throw new RuntimeException("Vé này không thuộc loại thanh toán online!");

        // 🔍 Tạo code thanh toán cần tìm (VD: CM-123)
        String paymentCode = "CM-" + ticket.getTicketId();

        // ✅ Gọi GoogleSheetsService
        Map<String, String> result = googleSheetsService.findTransactionByCode(paymentCode);
        if (!"true".equals(result.get("found")))
            throw new RuntimeException("Chưa tìm thấy giao dịch cho mã " + paymentCode);

        // 🔹 Kiểm tra số tiền
        String amountStr = result.get("amount").replaceAll("[^\\d]", "");
        BigDecimal paidAmount = new BigDecimal(amountStr);
        if (paidAmount.compareTo(ticket.getTotalPrice()) < 0)
            throw new RuntimeException("Số tiền chưa đủ (" + paidAmount + " < " + ticket.getTotalPrice() + ")");

        // ✅ Xác nhận thanh toán
        log.info("✅ Đã xác nhận giao dịch hợp lệ cho mã {}", paymentCode);

        // Gọi confirmPayment() để BOOK vé chính thức
        confirmPayment(ticketId, null, null);

        return ticketMapper.toResponse(ticket);
    }

//    public void confirmPayment(Integer ticketId) {
//        // ✅ Lấy danh sách combo đã lưu trong DB (để không bị mất)
//        List<TicketComboRequest> combos = ticketComboRepository.findByTicket_TicketId(ticketId)
//                .stream()
//                .map(tc -> new TicketComboRequest(tc.getCombo().getId(), tc.getQuantity()))
//                .collect(Collectors.toList());
//
//        // ✅ Gọi lại hàm đầy đủ để tính combo + discount + gửi mail đúng
//        confirmPayment(ticketId, combos, null);
//    }

    // ✅ Thay thế method confirmPayment(Integer ticketId) trong TicketService.java

    @Transactional
    public void confirmPayment(Integer ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy vé!"));

        // ⚠️ BỎ CHECK NÀY ĐI vì từ polling có thể đã BOOKED rồi
        // if (ticket.getTicketStatus() != Ticket.TicketStatus.HOLDING)
        //     throw new RuntimeException("Vé không ở trạng thái HOLDING!");

        // ✅ Kiểm tra nếu vé đã BOOKED thì bỏ qua
        if (ticket.getTicketStatus() == Ticket.TicketStatus.BOOKED) {
            log.info("ℹ️ Vé {} đã BOOKED rồi, không xử lý lại.", ticketId);
            return;
        }

        // ✅ Nếu vé đang HOLDING → tiến hành xác nhận
        if (ticket.getTicketStatus() == Ticket.TicketStatus.HOLDING) {
            // ✅ Lấy danh sách combo đã lưu trong DB
            List<TicketComboRequest> combos = ticketComboRepository.findByTicket_TicketId(ticketId)
                    .stream()
                    .map(tc -> new TicketComboRequest(tc.getCombo().getId(), tc.getQuantity()))
                    .collect(Collectors.toList());

            // ✅ Gọi hàm đầy đủ để tính combo + discount + gửi mail
            confirmPayment(ticketId, combos, null);
        } else {
            log.warn("⚠️ Vé {} có trạng thái {}, không thể xác nhận.", ticketId, ticket.getTicketStatus());
        }
    }

    // ============ GIỮ NGUYÊN method confirmPayment() đầy đủ bên dưới ============
    @Transactional
    public void confirmPayment(Integer ticketId, List<TicketComboRequest> combos, String customEmail) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy vé!"));

        // ✅ CHỈ kiểm tra HOLDING ở đây (method này được gọi từ confirm trực tiếp)
        if (ticket.getTicketStatus() != Ticket.TicketStatus.HOLDING) {
            log.warn("⚠️ Vé {} không ở trạng thái HOLDING, bỏ qua xác nhận.", ticketId);
            return;
        }

        // 🧩 Ép Hibernate load danh sách liên quan
        ticket.getTicketSeats().size();
        ticket.getTicketDiscounts().size();
        ticket.getTicketCombos().size();

        Showtime showtime = ticket.getShowtime();
        BigDecimal seatTotal = ticket.getTicketSeats().stream()
                .map(ts -> showtime.getPrice().multiply(ts.getSeat().getSeatType().getPriceMultiplier()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // ====================== 🍿 1️⃣ TẠO COMBO CHÍNH THỨC ======================
        BigDecimal comboTotal = BigDecimal.ZERO;

        List<TicketComboRequest> finalCombos;

        // ✅ Nếu combos null (ví dụ từ polling) → lấy từ DB
        if (combos == null || combos.isEmpty()) {
            finalCombos = ticket.getTicketCombos().stream()
                    .map(tc -> new TicketComboRequest(tc.getCombo().getId(), tc.getQuantity()))
                    .collect(Collectors.toList());
        } else {
            finalCombos = combos;
        }

        // 🔹 Xóa combo cũ (nếu có)
        List<TicketCombo> oldCombos = ticketComboRepository.findByTicket_TicketId(ticketId);
        if (!oldCombos.isEmpty()) {
            ticketComboRepository.deleteAll(oldCombos);
            log.info("🧹 Đã xóa combo cũ của vé {}", ticketId);
        }

        // 🔹 Tạo combo mới
        for (TicketComboRequest comboReq : finalCombos) {
            Combo combo = comboRepository.findById(comboReq.getComboId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy combo!"));

            BigDecimal subtotal = combo.getPrice()
                    .multiply(BigDecimal.valueOf(comboReq.getQuantity()));
            comboTotal = comboTotal.add(subtotal);

            TicketCombo ticketCombo = TicketCombo.builder()
                    .ticket(ticket)
                    .combo(combo)
                    .ticketId(ticket.getTicketId())
                    .comboId(combo.getId())
                    .quantity(comboReq.getQuantity())
                    .build();

            ticketComboRepository.save(ticketCombo);
        }

        // ====================== 🎁 2️⃣ TÍNH LẠI GIẢM GIÁ ======================
        BigDecimal discountTotal = ticket.getTicketDiscounts().stream()
                .map(TicketDiscount::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // ====================== 💰 3️⃣ TÍNH LẠI TỔNG TIỀN ======================
        BigDecimal total = seatTotal.add(comboTotal).subtract(discountTotal);
        if (total.compareTo(BigDecimal.ZERO) < 0) total = BigDecimal.ZERO;

        // 💾 Cập nhật vé
        ticket.setTotalPrice(total);
        ticket.setTicketStatus(Ticket.TicketStatus.BOOKED);
        ticket.setHoldUntil(null);
        ticketRepository.save(ticket);

        // ====================== 🔐 4️⃣ TẠO OTP & GỬI MAIL ======================
        String otpCode = createOtpForTicket(ticket);

        try {
            Account account = ticket.getAccount();
            Movie movie = showtime != null ? showtime.getPeriod().getMovie() : null;
            Auditorium auditorium = showtime != null ? showtime.getAuditorium() : null;
            Branch branch = (auditorium != null) ? auditorium.getBranch() : null;

            String recipient = (customEmail != null && !customEmail.isBlank())
                    ? customEmail
                    : (account != null ? account.getEmail() : ticket.getCustomerEmail());

            if (recipient == null || recipient.isBlank()) {
                log.warn("⚠️ Vé {} không có email hợp lệ, bỏ qua gửi mail.", ticket.getTicketId());
            } else {
                emailService.sendBookingConfirmationEmail(
                        recipient,
                        "CM-" + ticket.getTicketId(),
                        movie != null ? movie.getTitle() : "Không xác định",
                        auditorium != null ? auditorium.getName() : "Không xác định",
                        ticket.getTicketSeats().stream()
                                .map(ts -> ts.getSeat().getSeatRow() + ts.getSeat().getSeatNumber())
                                .collect(Collectors.joining(", ")),
                        showtime != null ? showtime.getStartTime() : LocalDateTime.now(),
                        comboTotal != null ? comboTotal : BigDecimal.ZERO,
                        seatTotal.add(comboTotal != null ? comboTotal : BigDecimal.ZERO),
                        discountTotal != null ? discountTotal : BigDecimal.ZERO,
                        ticket.getTotalPrice() != null ? ticket.getTotalPrice() : BigDecimal.ZERO,
                        (branch != null && branch.getAddress() != null) ? branch.getAddress() : "Không xác định",
                        "https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=ticket-" + ticket.getTicketId(),
                        otpCode,
                        ticketComboRepository.findByTicket_TicketId(ticketId).stream()
                                .map(tc -> String.format("%dx %s (%,.0f VND)",
                                        tc.getQuantity(),
                                        tc.getCombo().getNameCombo(),
                                        tc.getCombo().getPrice().multiply(BigDecimal.valueOf(tc.getQuantity()))))
                                .collect(Collectors.toList())
                );
                log.info("📧 Đã gửi mail xác nhận vé cho {}", recipient);
            }
        } catch (Exception e) {
            log.error("⚠️ Gửi mail thất bại nhưng KHÔNG rollback vé: {}", e.getMessage(), e);
            // ⚠️ KHÔNG throw exception để tránh rollback transaction
        }
    }



}
