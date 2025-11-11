package com.example.cinemaster.service;


import com.example.cinemaster.dto.request.DiscountRequest;
import com.example.cinemaster.dto.response.DiscountResponse;
import com.example.cinemaster.dto.response.TicketDiscountResponse;
import com.example.cinemaster.entity.*;
import com.example.cinemaster.entity.Discount.DiscountStatus;
import com.example.cinemaster.exception.AppException;
import com.example.cinemaster.exception.ErrorCode;
import com.example.cinemaster.mapper.DiscountMapper;
import com.example.cinemaster.repository.*;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class DiscountService {


    private final DiscountRepository discountRepository;
    private final DiscountMapper discountMapper;
    private final TicketRepository ticketRepository;
    private final TicketDiscountRepository ticketDiscountRepository;
    private final MembershipRepository membershipRepository;
    private final TicketSeatRepository ticketSeatRepository;
    private final TicketComboRepository ticketComboRepository;




    @Transactional
    public DiscountResponse create(DiscountRequest request) {
        if (discountRepository.existsByCode(request.getCode())) {
            throw new AppException(ErrorCode.DISCOUNT_CODE_EXISTS);
        }


        Discount discount = discountMapper.toEntity(request);
        discountRepository.save(discount);
        return discountMapper.toResponse(discount);
    }




    @Transactional(readOnly = true)
    public List<DiscountResponse> getAll() {
        List<Discount> discounts = discountRepository.findAll();
        discounts.forEach(this::autoUpdateStatus);


        return discounts.stream()
                .filter(d -> d.getDiscountStatus() != DiscountStatus.DELETED)
                .map(discountMapper::toResponse)
                .collect(Collectors.toList());
    }


    @Transactional(readOnly = true)
    public List<DiscountResponse> getByStatus(DiscountStatus status) {
        List<Discount> discounts = discountRepository.findAll();
        discounts.forEach(this::autoUpdateStatus);


        return discounts.stream()
                .filter(d -> d.getDiscountStatus() == status)
                .map(discountMapper::toResponse)
                .collect(Collectors.toList());
    }


    @Transactional(readOnly = true)
    public DiscountResponse getById(Integer id) {
        Discount discount = discountRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.DISCOUNT_NOT_FOUND));


        autoUpdateStatus(discount);
        return discountMapper.toResponse(discount);
    }


    @Transactional(readOnly = true)
    public DiscountResponse update(Integer id, DiscountRequest request) {
        Discount existing = discountRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.DISCOUNT_NOT_FOUND));


        if (!existing.getCode().equalsIgnoreCase(request.getCode()) &&
                discountRepository.existsByCode(request.getCode())) {
            throw new AppException(ErrorCode.DISCOUNT_CODE_EXISTS);
        }


        discountMapper.updateDiscountFromRequest(request, existing);
        autoUpdateStatus(existing);
        discountRepository.save(existing);


        return discountMapper.toResponse(existing);
    }




    @Transactional
    public void softDelete(Integer id) {
        Discount discount = discountRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.DISCOUNT_NOT_FOUND));


        discount.setDiscountStatus(DiscountStatus.INACTIVE);
        discountRepository.save(discount);
    }


    @Transactional
    public void restore(Integer id) {
        Discount discount = discountRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.DISCOUNT_NOT_FOUND));


        if (discount.getDiscountStatus() != DiscountStatus.INACTIVE) {
            throw new AppException(ErrorCode.INVALID_DISCOUNT);
        }


        if (discount.getExpiryDate() != null &&
                discount.getExpiryDate().isBefore(LocalDate.now())) {
            discount.setDiscountStatus(DiscountStatus.EXPIRED);
        } else {
            discount.setDiscountStatus(DiscountStatus.ACTIVE);
        }


        discountRepository.save(discount);
    }


    @Transactional
    public void hardDelete(Integer id) {
        Discount discount = discountRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.DISCOUNT_NOT_FOUND));


        discount.setDiscountStatus(DiscountStatus.DELETED);
        discountRepository.save(discount);
    }




    @Transactional
    private void autoUpdateStatus(Discount discount) {
        if (discount.getExpiryDate() == null) return;


        boolean expired = discount.getExpiryDate().isBefore(LocalDate.now());
        DiscountStatus status = discount.getDiscountStatus();


        if (expired && status != DiscountStatus.EXPIRED && status != DiscountStatus.DELETED) {
            discount.setDiscountStatus(DiscountStatus.EXPIRED);
            discountRepository.save(discount);
        }
    }


    @Transactional
    public TicketDiscountResponse applyDiscount(Integer ticketId, String code) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new AppException(ErrorCode.TICKET_NOT_FOUND));


        if (ticket.getAccount() == null)
            throw new AppException(ErrorCode.UNAUTHORIZED);


        if (ticket.getTicketStatus() != Ticket.TicketStatus.HOLDING)
            throw new AppException(ErrorCode.INVALID_TICKET_STATUS);


        Discount discount = discountRepository.findByCode(code)
                .orElseThrow(() -> new AppException(ErrorCode.DISCOUNT_NOT_FOUND));


        log.info("🎯 Discount [{}] - RequiredLevel={}", discount.getCode(),
                discount.getRequiredLevel() != null ? discount.getRequiredLevel().getLevelName() : "null");


        if (discount.getDiscountStatus() != Discount.DiscountStatus.ACTIVE)
            throw new AppException(ErrorCode.INVALID_DISCOUNT);


        if (discount.getExpiryDate() != null && discount.getExpiryDate().isBefore(LocalDate.now()))
            throw new AppException(ErrorCode.DISCOUNT_EXPIRED);


        Integer accountId = ticket.getAccount().getAccountID();


        // ======================== 🧱 Giới hạn sử dụng ========================
        if (discount.getMaxUsage() != null) {
            long totalUsage = ticketDiscountRepository.countByDiscount_DiscountID(discount.getDiscountID());
            if (totalUsage >= discount.getMaxUsage())
                throw new AppException(ErrorCode.DISCOUNT_LIMIT_REACHED);
        }
        if (discount.getMaxUsagePerAccount() != null) {
            long usedByAccount = ticketDiscountRepository
                    .countByDiscount_DiscountIDAndTicket_Account_AccountID(discount.getDiscountID(), accountId);
            if (usedByAccount >= discount.getMaxUsagePerAccount())
                throw new AppException(ErrorCode.DISCOUNT_LIMIT_REACHED);
        }
        if (discount.getMaxUsagePerDay() != null) {
            LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
            LocalDateTime endOfDay = startOfDay.plusDays(1);
            long usedToday = ticketDiscountRepository.countDailyUsage(
                    discount.getDiscountID(), accountId, startOfDay, endOfDay);
            if (usedToday >= discount.getMaxUsagePerDay())
                throw new AppException(ErrorCode.DISCOUNT_LIMIT_REACHED);
        }


        // ======================== 💰 Tính tổng gốc (seat + combo) ========================
        BigDecimal seatTotal = BigDecimal.ZERO;
        BigDecimal comboTotal = BigDecimal.ZERO;


        // ✅ Tính runtime từ TicketSeat và TicketCombo (dù là transient)
        if (ticket.getTicketSeats() != null && !ticket.getTicketSeats().isEmpty()) {
            seatTotal = ticket.getTicketSeats().stream()
                    .map(ts -> ts.getSeat().getSeatType().getPriceMultiplier()
                            .multiply(ticket.getShowtime().getPrice()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }


        if (ticket.getTicketCombos() != null && !ticket.getTicketCombos().isEmpty()) {
            comboTotal = ticket.getTicketCombos().stream()
                    .map(tc -> tc.getCombo().getPrice()
                            .multiply(BigDecimal.valueOf(tc.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }


        BigDecimal baseTotal = seatTotal.add(comboTotal);


        // Fallback nếu vé cũ chưa có tách giá riêng
        if (baseTotal.compareTo(BigDecimal.ZERO) <= 0 && ticket.getTotalPrice() != null) {
            baseTotal = ticket.getTotalPrice();
        }


        // ======================== 🧾 Kiểm tra điều kiện discount ========================
        if (discount.getMinOrderAmount() != null &&
                baseTotal.compareTo(discount.getMinOrderAmount()) < 0)
            throw new AppException(ErrorCode.DISCOUNT_MIN_ORDER_NOT_MET);


        Membership membership = membershipRepository.findByAccount_AccountID(accountId).orElse(null);
        if (discount.getRequiredLevel() != null) {
            int requiredPoints = discount.getRequiredLevel().getMinPoints();
            if (membership == null) {
                log.warn("⚠️ Không có membership, vẫn cho phép dùng mã công khai {}", discount.getCode());
            } else if (membership.getPoints() < requiredPoints) {
                throw new AppException(ErrorCode.MEMBERSHIP_LEVEL_TOO_LOW);
            }
        }


        // ======================== 💸 Tính và áp dụng giảm giá ========================
        BigDecimal discountValue = discount.getValue(baseTotal);
        if (discountValue.compareTo(BigDecimal.ZERO) <= 0)
            throw new AppException(ErrorCode.INVALID_DISCOUNT_VALUE);


        // Xóa giảm giá cũ (nếu có)
        if (!ticket.getTicketDiscounts().isEmpty()) {
            ticketDiscountRepository.deleteAll(ticket.getTicketDiscounts());
            ticket.getTicketDiscounts().clear();
        }


        TicketDiscount ticketDiscount = TicketDiscount.builder()
                .ticket(ticket)
                .discount(discount)
                .ticketId(ticket.getTicketId())
                .discountId(discount.getDiscountID())
                .amount(discountValue)
                .build();


        ticket.getTicketDiscounts().add(ticketDiscount);
        ticketDiscountRepository.save(ticketDiscount);


        BigDecimal newTotal = baseTotal.subtract(discountValue);
        if (newTotal.compareTo(BigDecimal.ZERO) < 0)
            newTotal = BigDecimal.ZERO;


        // ✅ Không cần lưu seat/combo xuống DB vì là @Transient
        ticket.setTotalPrice(newTotal);
        ticketRepository.saveAndFlush(ticket);


        log.info("✅ Discount [{}] applied. Seat={}, Combo={}, Base={}, Discount={}, New Total={}",
                discount.getCode(), seatTotal, comboTotal, baseTotal, discountValue, newTotal);


        // ======================== 🎯 Trả response cho FE ========================
        return TicketDiscountResponse.builder()
                .ticketId(ticket.getTicketId())
                .discountCode(discount.getCode())
                .discountAmount(discountValue)
                .originalTotal(baseTotal)
                .newTotal(newTotal)
                .seatPrice(seatTotal)
                .comboPrice(comboTotal)
                .build();
    }


}

