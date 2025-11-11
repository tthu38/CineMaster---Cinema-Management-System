
package com.example.cinemaster.controller;


import com.example.cinemaster.entity.Payment;
import com.example.cinemaster.entity.Ticket;
import com.example.cinemaster.repository.PaymentRepository;
import com.example.cinemaster.repository.TicketRepository;
import com.example.cinemaster.service.GoogleSheetsService;
import com.example.cinemaster.service.PaymentService;
import com.example.cinemaster.service.TicketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;




@RestController
@RequestMapping("/api/v1/payments")
@Slf4j
public class PaymentController {


    private final GoogleSheetsService sheetsService;
    private final PaymentService paymentService;
    private final TicketService ticketService;
    private final PaymentRepository paymentRepository;
    private final TicketRepository ticketRepository;


    private final ConcurrentHashMap<String, String> statusMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> orderTicketMap = new ConcurrentHashMap<>();


    public PaymentController(GoogleSheetsService sheetsService,
                             PaymentService paymentService,
                             TicketService ticketService,
                             PaymentRepository paymentRepository,
                             TicketRepository ticketRepository) {
        this.sheetsService = sheetsService;
        this.paymentService = paymentService;
        this.ticketService = ticketService;
        this.paymentRepository = paymentRepository;
        this.ticketRepository = ticketRepository;
    }


    // ====================  TẠO ĐƠN HÀNG ====================
    @PostMapping("/create")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> body) {
        long amount;
        Integer ticketId;
        try {
            amount = Long.parseLong(body.get("amount").toString());
            ticketId = Integer.parseInt(body.get("ticketId").toString());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "invalid data"));
        }

        String code = "CINE" + UUID.randomUUID().toString().replaceAll("-", "").substring(0, 8).toUpperCase();
        statusMap.put(code, "pending");
        orderTicketMap.put(code, ticketId);


        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy vé để liên kết Payment!"));

        Payment payment = Payment.builder()
                .orderCode(code)
                .ticketID(ticket)
                .amount(BigDecimal.valueOf(amount))
                .status("PENDING")
                .description("Đơn hàng khởi tạo — chưa thanh toán")
                .createdAt(LocalDateTime.now())
                .build();


        paymentRepository.save(payment);
        log.info("Đã tạo Payment [{}] cho TicketID {}", code, ticketId);


        return ResponseEntity.ok(Map.of(
                "code", code,
                "ticketId", ticketId,
                "amount", amount,
                "accountNumber", "0345506824",
                "bankName", "MBBank",
                "noteHint", "Vui lòng ghi nội dung chuyển khoản: " + code,
                "pollUrl", "/api/v1/payments/status/" + code
        ));
    }


    // ==================== KIỂM TRA TRẠNG THÁI THANH TOÁN ====================
    @GetMapping("/status/{code}")
    public ResponseEntity<?> checkStatus(@PathVariable String code) {
        try {
            Map<String, String> r = sheetsService.findTransactionByCode(code);


            if ("true".equals(r.get("found"))) {
                statusMap.put(code, "paid");


                Integer ticketId = orderTicketMap.get(code);
                if (ticketId != null) {
                    Ticket ticket = ticketRepository.findById(ticketId)
                            .orElseThrow(() -> new RuntimeException("Không tìm thấy vé!"));

                    if (ticket.getTicketStatus() == Ticket.TicketStatus.HOLDING) {
                        BigDecimal amount = new BigDecimal(r.getOrDefault("amount", "0"));
                        String note = r.getOrDefault("note", "Thanh toán thành công qua Google Sheets");


                        paymentService.confirmPaid(code, note, amount, ticketId);


                        try {
                            String customerEmail = ticket.getAccount() != null ? ticket.getAccount().getEmail() : ticket.getCustomerEmail();

                            ticketService.confirmPayment(ticketId, null, customerEmail);


                            log.info(" Đã xác nhận & gửi mail vé {} đến {}", ticketId, customerEmail);
                        } catch (Exception mailError) {
                            log.error(" Lỗi gửi mail cho vé {}: {}", ticketId, mailError.getMessage(), mailError);
                        }




                    } else {
                        log.info("Vé {} đã BOOKED rồi, bỏ qua xử lý lại.", ticketId);
                    }
                }


                return ResponseEntity.ok(Map.of("status", "paid", "meta", r));
            } else {
                return ResponseEntity.ok(Map.of("status", "pending"));
            }


        } catch (Exception e) {
            log.error("Lỗi checkStatus cho mã {}: {}", code, e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}

