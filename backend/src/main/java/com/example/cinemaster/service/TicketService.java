package com.example.cinemaster.service;

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
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionTemplate;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
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
    private final TransactionTemplate transactionTemplate;



    /* 🟢 Tạo hoặc cập nhật vé tạm */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TicketResponse createOrUpdateTicket(TicketCreateRequest req) {
        Account account = accountRepository.findById(req.getAccountId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản!"));
        Showtime showtime = showtimeRepository.findById(req.getShowtimeId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy suất chiếu!"));


        Ticket ticket;
        Integer ticketId = req.getTicketId();


        if (ticketId != null && ticketId > 0) {
            ticket = ticketRepository.findById(ticketId).orElse(null);


            boolean invalid =
                    (ticket == null)
                            || !ticket.getAccount().getAccountID().equals(req.getAccountId())
                            || ticket.getTicketStatus() != Ticket.TicketStatus.HOLDING
                            || (ticket.getHoldUntil() != null && ticket.getHoldUntil().isBefore(LocalDateTime.now()));


            if (invalid) {
                log.warn("⚠️ Vé {} không hợp lệ hoặc hết hạn, tạo mới vé.", ticketId);
                ticket = createNewHoldingTicket(account, showtime, req.getCustomerEmail());
            } else {
                ensureListNotNull(ticket);
                ticket.setHoldUntil(LocalDateTime.now().plusMinutes(5));
                if (req.getCustomerEmail() != null && !req.getCustomerEmail().isBlank())
                    ticket.setCustomerEmail(req.getCustomerEmail());
                ticketRepository.save(ticket);
            }
        } else {
            ticket = ticketRepository
                    .findTopByAccount_AccountIDAndTicketStatusOrderByBookingTimeDesc(
                            req.getAccountId(), Ticket.TicketStatus.HOLDING)
                    .orElseGet(() -> createNewHoldingTicket(account, showtime, req.getCustomerEmail()));


            ensureListNotNull(ticket);
            ticket.setShowtime(showtime);
            ticket.setHoldUntil(LocalDateTime.now().plusMinutes(5));
            ticketRepository.save(ticket);
        }


        ticket.getTicketSeats().size();
        ticket.getTicketCombos().size();
        ticket.getTicketDiscounts().size();


        // ====================== 💺 2️⃣ Xử lý GHẾ ======================
        Showtime currentShowtime = ticket.getShowtime();


        List<Integer> occupiedSeats = ticketRepository.findOccupiedSeatIdsByShowtimeExcludeTicket(
                currentShowtime.getShowtimeID(), ticket.getTicketId());


        BigDecimal seatTotal = BigDecimal.ZERO;
        ticket.getTicketSeats().clear();


        for (Integer seatId : req.getSeatIds()) {
            if (occupiedSeats.contains(seatId))
                throw new RuntimeException("Ghế " + seatId + " đã được giữ hoặc đặt!");
            Seat seat = seatRepository.findById(seatId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy ghế ID=" + seatId));


            BigDecimal seatPrice = currentShowtime.getPrice()
                    .multiply(seat.getSeatType().getPriceMultiplier());
            seatTotal = seatTotal.add(seatPrice);
            ticket.getTicketSeats().add(new TicketSeat(ticket, seat));
        }


        // ====================== 🍿 3️⃣ Xử lý COMBO ======================
        BigDecimal comboTotal = BigDecimal.ZERO;
        ticket.getTicketCombos().clear();


        if (req.getCombos() != null && !req.getCombos().isEmpty()) {
            for (TicketCreateRequest.ComboItem comboReq : req.getCombos()) {
                Combo combo = comboRepository.findById(comboReq.getComboId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy combo ID=" + comboReq.getComboId()));


                BigDecimal subtotal = combo.getPrice()
                        .multiply(BigDecimal.valueOf(comboReq.getQuantity()));
                comboTotal = comboTotal.add(subtotal);


                TicketComboKey key = new TicketComboKey(ticket.getTicketId(), combo.getId());
                TicketCombo tc = TicketCombo.builder()
                        .id(key)
                        .ticket(ticket)
                        .combo(combo)
                        .quantity(comboReq.getQuantity())
                        .build();


                ticket.getTicketCombos().add(tc);
            }
        }


        // ====================== 🎁 4️⃣ Áp dụng hoặc cập nhật DISCOUNT ======================
        BigDecimal discountTotal = BigDecimal.ZERO;
        BigDecimal totalBeforeDiscount = seatTotal.add(comboTotal);


        if (req.getDiscountIds() != null && !req.getDiscountIds().isEmpty()) {
            // ✅ FE gửi discount mới → xóa cũ, tính lại mới
            ticket.getTicketDiscounts().clear();
            for (Integer discountId : req.getDiscountIds()) {
                Discount discount = discountRepository.findById(discountId)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy discount ID=" + discountId));


                BigDecimal discountValue = discount.getValue(totalBeforeDiscount);
                discountTotal = discountTotal.add(discountValue);


                TicketDiscount td = TicketDiscount.builder()
                        .ticket(ticket)
                        .discount(discount)
                        .amount(discountValue)
                        .build();
                ticket.getTicketDiscounts().add(td);
            }
        } else if (ticket.getTicketDiscounts() != null && !ticket.getTicketDiscounts().isEmpty()) {
            // ✅ Giữ discount cũ nhưng tính lại theo tổng mới
            discountTotal = BigDecimal.ZERO;
            for (TicketDiscount td : ticket.getTicketDiscounts()) {
                Discount discount = td.getDiscount();
                BigDecimal newDiscountValue = discount.getValue(totalBeforeDiscount);
                td.setAmount(newDiscountValue);
                discountTotal = discountTotal.add(newDiscountValue);
            }
        }


        // ====================== 💰 5️⃣ Tính & lưu tổng tiền ======================
        BigDecimal total = totalBeforeDiscount.subtract(discountTotal);
        if (total.compareTo(BigDecimal.ZERO) < 0) total = BigDecimal.ZERO;


        ticket.setTotalPrice(total);
        ticketRepository.saveAndFlush(ticket);


        log.info("💰 Vé {} cập nhật thành công | Ghế={} | Combo={} | Giảm={} | Tổng={} | Email={}",
                ticket.getTicketId(), seatTotal, comboTotal, discountTotal, total, ticket.getCustomerEmail());


        return ticketMapper.toResponse(ticket);
    }


    /** ✅ Helper: tạo vé mới đang HOLDING */
    private Ticket createNewHoldingTicket(Account account, Showtime showtime, String email) {
        Ticket newTicket = Ticket.builder()
                .account(account)
                .showtime(showtime)
                .ticketStatus(Ticket.TicketStatus.HOLDING)
                .paymentMethod(Ticket.PaymentMethod.CASH)
                .holdUntil(LocalDateTime.now().plusMinutes(5))
                .customerEmail(email)
                .build();
        ensureListNotNull(newTicket);
        ticketRepository.save(newTicket);
        return newTicket;
    }


    /** ✅ Đảm bảo list trong vé không null */
    private void ensureListNotNull(Ticket ticket) {
        if (ticket.getTicketSeats() == null) ticket.setTicketSeats(new ArrayList<>());
        if (ticket.getTicketCombos() == null) ticket.setTicketCombos(new ArrayList<>());
        if (ticket.getTicketDiscounts() == null) ticket.setTicketDiscounts(new ArrayList<>());
    }

    /* 🟢 Đổi ghế khi quay lại */
    @Transactional
    public TicketResponse replaceSeats(Integer ticketId, List<Integer> newSeatIds) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy vé!"));


        // ✅ Chỉ đổi khi vé còn HOLDING
        if (ticket.getTicketStatus() != Ticket.TicketStatus.HOLDING)
            throw new RuntimeException("Chỉ được đổi ghế khi vé đang HOLDING!");


        Showtime showtime = ticket.getShowtime();


        // ✅ Lấy danh sách ghế đã bị giữ hoặc đặt, nhưng loại trừ chính vé hiện tại
        List<Integer> occupiedSeats = ticketRepository.findOccupiedSeatIdsByShowtimeExcludeTicket(
                showtime.getShowtimeID(), ticket.getTicketId());


        // ✅ Xóa ghế cũ trước khi thêm mới
        ticket.getTicketSeats().clear();
        BigDecimal total = BigDecimal.ZERO;


        for (Integer seatId : newSeatIds) {
            if (occupiedSeats.contains(seatId)) {
                throw new RuntimeException("Ghế " + seatId + " đã được giữ hoặc đặt!");
            }


            Seat seat = seatRepository.findById(seatId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy ghế ID=" + seatId));


            BigDecimal seatPrice = showtime.getPrice()
                    .multiply(seat.getSeatType().getPriceMultiplier());


            total = total.add(seatPrice);
            ticket.getTicketSeats().add(new TicketSeat(ticket, seat));
        }


        // ✅ Gia hạn thời gian giữ vé thêm 5 phút
        ticket.setHoldUntil(LocalDateTime.now().plusMinutes(5));
        ticket.setTotalPrice(total);
        ticketRepository.saveAndFlush(ticket);


        log.info("✅ Vé {} đổi ghế thành công | Tổng tiền mới: {} | Số ghế: {}",
                ticketId, total, newSeatIds.size());


        return ticketMapper.toResponse(ticket);
    }


    private String createOtpForTicket(Ticket ticket) {
        try {
            if (ticket == null) {
                throw new IllegalArgumentException("Ticket không được null khi tạo OTP");
            }

            // ✅ Xóa OTP cũ của vé này (nếu có)
            List<Otp> existingOtps = otpRepository.findByTicket(ticket);
            if (existingOtps != null && !existingOtps.isEmpty()) {
                otpRepository.deleteAll(existingOtps);
                otpRepository.flush();
                log.info("🧹 Đã xóa {} OTP cũ của vé {}", existingOtps.size(), ticket.getTicketId());
            }

            // ✅ Sinh OTP ngẫu nhiên 6 chữ số an toàn bằng SecureRandom
            SecureRandom random = new SecureRandom();
            String otpValue = String.format("%06d", random.nextInt(1_000_000)); // → 000001–999999

            // ✅ Xác định thời gian hết hạn: dùng endTime nếu có, fallback = startTime + 2h
            LocalDateTime expiryTime = (ticket.getShowtime() != null && ticket.getShowtime().getEndTime() != null)
                    ? ticket.getShowtime().getEndTime()
                    : (ticket.getShowtime() != null ? ticket.getShowtime().getStartTime().plusHours(2)
                    : LocalDateTime.now().plusHours(2));

            // ✅ Tạo entity OTP
            Otp otp = Otp.builder()
                    .accountID(ticket.getAccount())
                    .ticket(ticket)
                    .code(otpValue)
                    .expiry(expiryTime)
                    .build();

            // ✅ Lưu vào DB
            otpRepository.saveAndFlush(otp);

            log.info("🔑 Đã tạo OTP {} cho vé {} (hết hạn lúc {})",
                    otpValue, ticket.getTicketId(), expiryTime);

            return otpValue;

        } catch (Exception e) {
            log.error("❌ Lỗi khi tạo OTP cho vé {}: {}",
                    (ticket != null ? ticket.getTicketId() : "NULL"), e.getMessage(), e);
            throw new RuntimeException("Không thể tạo OTP", e);
        }
    }




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


        // 🧹 Xoá combo cũ (nếu có)
        List<TicketCombo> oldCombos = ticketComboRepository.findByTicket_TicketId(ticketId);
        if (!oldCombos.isEmpty()) {
            ticketComboRepository.deleteAll(oldCombos);
            ticketComboRepository.flush();
            ticket.getTicketCombos().clear();
            log.info("🧹 Đã xoá combo cũ của vé {}", ticketId);
        }


        BigDecimal comboTotal = BigDecimal.ZERO;


        if (combos != null && !combos.isEmpty()) {
            for (var comboReq : combos) {
                Combo combo = comboRepository.findById(comboReq.getComboId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy combo!"));


                BigDecimal subtotal = combo.getPrice()
                        .multiply(BigDecimal.valueOf(comboReq.getQuantity()));
                comboTotal = comboTotal.add(subtotal);


                // ✅ Tạo khóa tổng hợp cho TicketCombo
                TicketComboKey key = new TicketComboKey(ticket.getTicketId(), combo.getId());


                TicketCombo tc = TicketCombo.builder()
                        .id(key)              // ✅ dùng id thay vì ticketId/comboId
                        .ticket(ticket)
                        .combo(combo)
                        .quantity(comboReq.getQuantity())
                        .build();


                ticketComboRepository.save(tc);
            }


            log.info("🍿 Vé {} đã thêm {} combo mới (tổng {}₫)", ticketId, combos.size(), comboTotal);
        } else {
            log.info("⚪ Vé {} không chọn combo nào, comboTotal=0", ticketId);
        }


        // 🧾 Cập nhật lại giá combo và tổng vé
        ticket.setComboPrice(comboTotal);


        BigDecimal seatPrice = ticket.getSeatPrice() != null ? ticket.getSeatPrice() : BigDecimal.ZERO;
        ticket.setTotalPrice(seatPrice.add(comboTotal));


        ticketRepository.saveAndFlush(ticket);
        log.info("✅ Vé {} cập nhật thành công, totalPrice={}₫", ticketId, ticket.getTotalPrice());
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
    // CUSTOMER: Lấy danh sách vé của người dùng
    public List<TicketResponse> getTicketsByAccount(Integer accountID) {
        return ticketRepository.findByAccount_AccountID(accountID).stream()
                .map(ticketMapper::toShortResponse)
                .collect(Collectors.toList());
    }



    // STAFF: Lấy danh sách vé theo chi nhánh
    public List<TicketResponse> getTicketsByBranch(Integer branchId) {
        return ticketRepository.findByBranch(branchId).stream()
                .map(ticketMapper::toResponse)
                .collect(Collectors.toList());
    }

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



    public TicketResponse verifyOnlinePayment(Integer ticketId) throws Exception {
        // 🔍 1️⃣ Lấy vé từ DB
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy vé!"));


        // ⚡ 2️⃣ Nếu vé đã BOOKED rồi thì bỏ qua xác nhận lại
        if (ticket.getTicketStatus() == Ticket.TicketStatus.BOOKED) {
            log.info("🎟️ Vé {} đã BOOKED rồi — bỏ qua xác nhận lại.", ticketId);
            return ticketMapper.toResponse(ticket);
        }


        // ❌ Nếu vé bị hủy hoặc hoàn tiền thì không hợp lệ
        if (ticket.getTicketStatus() == Ticket.TicketStatus.CANCELLED
                || ticket.getTicketStatus() == Ticket.TicketStatus.REFUNDED) {
            throw new RuntimeException("Vé này không hợp lệ để xác nhận thanh toán!");
        }


        // 🧭 3️⃣ Chỉ xử lý vé HOLDING
        if (ticket.getTicketStatus() != Ticket.TicketStatus.HOLDING)
            throw new RuntimeException("Chỉ kiểm tra thanh toán cho vé HOLDING!");


        // 💳 4️⃣ Kiểm tra phương thức thanh toán
        if (ticket.getPaymentMethod() != Ticket.PaymentMethod.ONLINE)
            throw new RuntimeException("Vé này không thuộc loại thanh toán online!");


        // 📄 5️⃣ Tạo mã thanh toán (VD: CM-123)
        String paymentCode = "CM-" + ticket.getTicketId();


        // 🔗 6️⃣ Kiểm tra Google Sheets (xem có giao dịch nào khớp không)
        Map<String, String> result = googleSheetsService.findTransactionByCode(paymentCode);
        if (!"true".equals(result.get("found"))) {
            log.warn("⏳ Chưa tìm thấy giao dịch cho mã {} trên Google Sheets.", paymentCode);
            throw new RuntimeException("Chưa tìm thấy giao dịch cho mã " + paymentCode);
        }


        // 💰 7️⃣ Kiểm tra số tiền thanh toán
        String amountStr = result.get("amount").replaceAll("[^\\d]", "");
        BigDecimal paidAmount = new BigDecimal(amountStr);
        if (paidAmount.compareTo(ticket.getTotalPrice()) < 0)
            throw new RuntimeException("Số tiền chưa đủ (" + paidAmount + " < " + ticket.getTotalPrice() + ")");


        // ✅ 8️⃣ Đánh dấu thanh toán hợp lệ
        log.info("✅ Đã xác nhận giao dịch hợp lệ cho mã {} (số tiền {})", paymentCode, paidAmount);


        // 💾 9️⃣ Gọi confirmPayment() để BOOK vé chính thức (có gửi mail & OTP)
        confirmPayment(ticketId, null, null);


        // 🧾 🔟 Ghi lại TicketHistory (HOLDING → BOOKED)
        saveTicketHistory(ticket,
                Ticket.TicketStatus.HOLDING.name(),
                Ticket.TicketStatus.BOOKED.name(),
                ticket.getAccount(),
                "Xác nhận thanh toán online thành công qua Google Sheets");


        // 🧩 11️⃣ Log & phản hồi
        log.info("🎟️ Vé {} đã được BOOKED qua xác nhận online, tổng tiền {}", ticketId, ticket.getTotalPrice());
        return ticketMapper.toResponse(ticket);
    }


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


        // ✅ Load toàn bộ quan hệ (ghế, combo, discount)
        forceLoadRelations(ticket);
        entityManager.refresh(ticket); // đảm bảo dữ liệu mới nhất từ applyDiscount


        // ================= ⚙️ 1️⃣ Kiểm tra trạng thái vé =================
        if (ticket.getTicketStatus() == Ticket.TicketStatus.BOOKED) {
            log.info("ℹ️ Vé {} đã BOOKED, gửi lại email xác nhận.", ticketId);
            sendBookingEmail(ticket, combos, customEmail, ticket.getShowtime());
            return;
        }


        if (ticket.getTicketStatus() != Ticket.TicketStatus.HOLDING) {
            log.warn("⚠️ Vé {} không ở trạng thái HOLDING, bỏ qua xác nhận.", ticketId);
            return;
        }


        Showtime showtime = ticket.getShowtime();


        // ================= 🍿 2️⃣ Cập nhật combo mới (nếu có) =================
        if (combos != null) {
            ticketComboRepository.deleteAll(ticketComboRepository.findByTicket_TicketId(ticketId));


            if (!combos.isEmpty()) {
                for (TicketComboRequest comboReq : combos) {
                    Combo combo = comboRepository.findById(comboReq.getComboId())
                            .orElseThrow(() -> new RuntimeException("Không tìm thấy combo ID: " + comboReq.getComboId()));


                    TicketCombo tc = TicketCombo.builder()
                            .id(new TicketComboKey(ticket.getTicketId(), combo.getId()))
                            .ticket(ticket)
                            .combo(combo)
                            .quantity(comboReq.getQuantity())
                            .build();


                    ticketComboRepository.save(tc);
                }
                ticketComboRepository.flush();
                log.info("🍿 Vé {} đã cập nhật {} combo mới từ FE", ticketId, combos.size());
            } else {
                log.info("🍿 Vé {} không chọn combo → giữ trống hoàn toàn", ticketId);
            }


            // ✅ Reload lại combos vừa insert (để tính total đúng)
            ticket.getTicketCombos().clear();
            ticket.getTicketCombos().addAll(ticketComboRepository.findByTicket_TicketId(ticketId));
        }


        // ================= 💰 3️⃣ Tính lại total (seat + combo – discount) =================
        BigDecimal seatTotal = ticket.getTicketSeats().stream()
                .map(ts -> ts.getSeat().getSeatType().getPriceMultiplier()
                        .multiply(showtime.getPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);


        BigDecimal comboTotal = ticket.getTicketCombos().stream()
                .map(tc -> tc.getCombo().getPrice()
                        .multiply(BigDecimal.valueOf(tc.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);


        BigDecimal discountTotal = BigDecimal.ZERO;
        if (ticket.getTicketDiscounts() != null && !ticket.getTicketDiscounts().isEmpty()) {
            for (TicketDiscount td : ticket.getTicketDiscounts()) {
                discountTotal = discountTotal.add(td.getAmount());
            }
        }


        BigDecimal totalPaid = seatTotal.add(comboTotal).subtract(discountTotal);
        if (totalPaid.compareTo(BigDecimal.ZERO) < 0) totalPaid = BigDecimal.ZERO;


        ticket.setTotalPrice(totalPaid);


        // ================= 🧾 4️⃣ Cập nhật trạng thái vé =================
        ticket.setTicketStatus(Ticket.TicketStatus.BOOKED);
        ticket.setHoldUntil(null);
        ticketRepository.saveAndFlush(ticket);


        saveTicketHistory(ticket,
                Ticket.TicketStatus.HOLDING.name(),
                Ticket.TicketStatus.BOOKED.name(),
                ticket.getAccount(),
                "Thanh toán online thành công");


        // ================= 🔐 5️⃣ Sinh OTP =================
        String otp = createOtpForTicket(ticket);


        // ================= 🏆 6️⃣ Cộng điểm Membership =================
        try {
            long points = totalPaid.divide(BigDecimal.valueOf(1000), RoundingMode.DOWN).longValue();
            if (points > 0 && ticket.getAccount() != null) {
                membershipService.updateMembershipAfterPayment(ticket.getAccount(), (int) points);
                log.info("🏅 Cộng {} điểm cho tài khoản {}", points, ticket.getAccount().getAccountID());
            }
        } catch (Exception ex) {
            log.error("⚠️ Lỗi khi cộng điểm vé {}: {}", ticketId, ex.getMessage(), ex);
        }


        // ================= 📧 7️⃣ Gửi email xác nhận =================
        sendBookingEmail(ticket, combos, customEmail, showtime);


        log.info("✅ Vé {} BOOKED thành công | Tổng tiền {} | Giảm {} | OTP {}",
                ticketId, totalPaid, discountTotal, otp);
    }






    /** Ép Hibernate load toàn bộ quan hệ cần thiết */
    private void forceLoadRelations(Ticket ticket) {
        ticket.getTicketSeats().size();
        ticket.getTicketCombos().size();
        ticket.getTicketDiscounts().size();
    }

    private void sendBookingEmail(
            Ticket ticket,
            List<TicketComboRequest> combos,
            String customEmail,
            Showtime showtime
    ) {
        try {
            // ✅ TỔNG TIỀN THẬT (đã thanh toán, đã lưu trong DB)
            BigDecimal totalPrice = ticket.getTotalPrice();

            // ✅ Tính tổng combo
            BigDecimal comboTotal = ticket.getTicketCombos().stream()
                    .map(tc -> tc.getCombo().getPrice().multiply(BigDecimal.valueOf(tc.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // ✅ Tổng giảm giá
            BigDecimal discountTotal = ticket.getTicketDiscounts().stream()
                    .map(TicketDiscount::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // ✅ Tổng tiền ghế
            BigDecimal seatTotal = ticket.getTicketSeats().stream()
                    .map(ts -> showtime.getPrice().multiply(ts.getSeat().getSeatType().getPriceMultiplier()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // ✅ Giá gốc
            BigDecimal originalPrice = seatTotal.add(comboTotal);

            // ✅ Chi tiết combo
            List<String> comboDetails = ticket.getTicketCombos().stream()
                    .map(tc -> String.format("%s x%d = %,.0f VND",
                            tc.getCombo().getNameCombo(),
                            tc.getQuantity(),
                            tc.getCombo().getPrice().multiply(BigDecimal.valueOf(tc.getQuantity())).doubleValue()))
                    .collect(Collectors.toList());

            // ✅ Tên ghế
            String seatNames = ticket.getTicketSeats().stream()
                    .map(ts -> ts.getSeat().getSeatNumber())
                    .collect(Collectors.joining(", "));

            // ✅ Lấy OTP từ DB (nếu không có thì tạo mới)
            String otpCode = getOrCreateOtpCode(ticket);

            // ✅ QR Code URL
            String qrCodeUrl = getQrCodeUrl(ticket);

            // ✅ Email người nhận
            String recipientEmail = (customEmail != null && !customEmail.trim().isEmpty())
                    ? customEmail
                    : ticket.getAccount().getEmail();

            // ✅ Mã vé
            String ticketCode = "#" + ticket.getTicketId();

            // ✅ Gửi email
            emailService.sendBookingConfirmationEmail(
                    recipientEmail,
                    ticketCode,
                    showtime.getMovie().getTitle(),
                    showtime.getAuditorium().getName(),
                    seatNames,
                    showtime.getStartTime(),
                    comboTotal,
                    originalPrice,
                    discountTotal,
                    totalPrice,
                    showtime.getAuditorium().getBranch().getAddress(),
                    qrCodeUrl,
                    otpCode,
                    comboDetails
            );

            log.info("📧 Đã gửi email xác nhận vé {} cho {} (Tổng: {} VND, OTP: {})",
                    ticket.getTicketId(), recipientEmail, totalPrice, otpCode);

        } catch (Exception e) {
            log.error("❌ Lỗi khi gửi email cho vé {}: {}", ticket.getTicketId(), e.getMessage(), e);
            throw new RuntimeException("Không thể gửi email xác nhận", e);
        }
    }
    private String getOrCreateOtpCode(Ticket ticket) {
        // ✅ Kiểm tra xem DB đã có OTP cho vé này chưa
        List<Otp> otps = otpRepository.findByTicket(ticket);

        if (otps != null && !otps.isEmpty()) {
            String existingOtp = otps.get(0).getCode();
            if (existingOtp != null && !existingOtp.trim().isEmpty()) {
                log.info("🔑 OTP hiện có cho vé {} là {}", ticket.getTicketId(), existingOtp);
                return existingOtp;
            }
        }

        // ✅ Nếu chưa có thì tạo mới OTP
        String newOtp = createOtpForTicket(ticket);
        log.info("🆕 Tạo mới OTP {} cho vé {}", newOtp, ticket.getTicketId());
        return newOtp;
    }


    /**
     * Lấy OTP code mới nhất từ DB
     */
    private String getOtpCode(Ticket ticket) {
        List<Otp> otps = otpRepository.findByTicket(ticket);


        if (otps.isEmpty()) {
            log.warn("⚠️ Không tìm thấy OTP cho vé {}", ticket.getTicketId());
            return "000000";
        }


        // ✅ Lấy OTP mới nhất (có expiry xa nhất)
        Otp latestOtp = otps.stream()
                .max((o1, o2) -> {
                    if (o1.getExpiry() == null) return -1;
                    if (o2.getExpiry() == null) return 1;
                    return o1.getExpiry().compareTo(o2.getExpiry());
                })
                .orElse(otps.get(0));


        return latestOtp.getCode();
    }


    private String getQrCodeUrl(Ticket ticket) {
        try {
            // ✅ Lấy OTP từ DB
            String otpCode = getOtpCode(ticket);


            // ✅ Tạo dữ liệu QR: ticketId + OTP
            String qrData = String.format("TICKET_ID:%d|OTP:%s",
                    ticket.getTicketId(),
                    otpCode);


            // ✅ Encode URL
            String encodedData = java.net.URLEncoder.encode(qrData, java.nio.charset.StandardCharsets.UTF_8);


            // ✅ Tạo QR code URL (dùng API miễn phí)
            String qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=" + encodedData;


            log.info("🔲 QR Code URL cho vé {}: {}", ticket.getTicketId(), qrUrl);
            return qrUrl;


        } catch (Exception e) {
            log.error("❌ Lỗi tạo QR code cho vé {}: {}", ticket.getTicketId(), e.getMessage(), e);
            return ""; // Trả về empty nếu lỗi
        }
    }



}
