package com.example.cinemaster.service;


import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;


@Slf4j
@Service
public class EmailService {


    @Autowired
    private JavaMailSender mailSender;


    @Value("${app.frontend.reset-password-url}")
    private String resetPasswordUrl;




    public void sendVerificationEmail(String to, String code) throws MessagingException {
        String subject = "Xác thực tài khoản";
        String content = "<h3>Chào bạn!</h3>"
                + "<p>Mã xác thực của bạn là: <b>" + code + "</b></p>"
                + "<p>Mã này sẽ hết hạn sau 10 phút.</p>";


        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(content, true);


        mailSender.send(message);
    }


    public void sendPasswordResetEmail(String to, String token) throws MessagingException {
        String subject = "Đặt lại mật khẩu";
        String link = resetPasswordUrl + "?token=" + token;


        String content = """
               <div style="font-family: Arial, sans-serif; line-height:1.6;">
                 <h3>Yêu cầu đặt lại mật khẩu</h3>
                 <p>Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn.</p>
                 <p>Bấm vào nút bên dưới để đặt lại mật khẩu (hết hạn sau 10–15 phút):</p>
                 <p>
                   <a href="%s"
                      style="display:inline-block;padding:10px 16px;text-decoration:none;border-radius:6px;
                             background:#2563eb;color:#fff;font-weight:600">
                     Đặt lại mật khẩu
                   </a>
                 </p>
                 <p>Nếu nút không hoạt động, hãy copy link sau vào trình duyệt:</p>
                 <p><a href="%s">%s</a></p>
                 <hr/>
                 <small>Nếu bạn không yêu cầu đặt lại mật khẩu, hãy bỏ qua email này.</small>
               </div>
               """.formatted(link, link, link);


        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(content, true);


        mailSender.send(message);
    }


    public void sendOtpForChangeEmail(String to, String code) throws MessagingException {
        String subject = "Xác nhận thay đổi email";
        String content = "<h3>Xin chào!</h3>"
                + "<p>Bạn đã yêu cầu thay đổi email đăng nhập.</p>"
                + "<p>Mã xác thực của bạn là: <b>" + code + "</b></p>"
                + "<p>Mã này sẽ hết hạn sau 10 phút.</p>";


        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(content, true);


        mailSender.send(message);
    }


    //Counter Password
    @Value("${app.frontend.invite-password-url}")
    private String invitePasswordUrl;


    public void sendInviteEmail(String to, String token) throws MessagingException {
        String subject = "Thiết lập mật khẩu tài khoản của bạn";
        String link = invitePasswordUrl + "?token=" + token;


        String content = """
           <div style="font-family: Arial, sans-serif; line-height:1.6;">
             <h3>Xin chào!</h3>
             <p>Chúng tôi đã tạo tài khoản cho bạn. Vui lòng bấm nút bên dưới để đặt mật khẩu lần đầu:</p>
             <p>
               <a href="%s"
                  style="display:inline-block;padding:10px 16px;text-decoration:none;border-radius:6px;
                         background:#16a34a;color:#fff;font-weight:600">
                 Đặt mật khẩu
               </a>
             </p>
             <p>Nếu nút không hoạt động, copy link sau vào trình duyệt:</p>
             <p><a href="%s">%s</a></p>
             <hr/>
             <small>Liên kết này sẽ hết hạn sau 30 phút.</small>
           </div>
           """.formatted(link, link, link);


        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(content, true);


        mailSender.send(message);
    }


    public void sendBookingConfirmationEmail(
            String to,
            String reservationCode,
            String movieTitle,
            String auditoriumName,
            String seatNames,
            LocalDateTime showtime,
            BigDecimal comboTotal,
            BigDecimal originalPrice,
            BigDecimal discountTotal,
            BigDecimal totalPrice,
            String branchAddress,
            String qrCodeUrl,
            String otpCode,
            List<String> comboDetails
    ) throws MessagingException {


        String subject = " Vé xem phim của bạn tại CineMaster";


        // ✅ Bảo vệ null cho toàn bộ tham số
        String safeReservation = reservationCode != null ? reservationCode : "N/A";
        String safeOtp = otpCode != null ? otpCode : "000000";
        String safeQr = qrCodeUrl != null ? qrCodeUrl : "";
        String safeMovie = movieTitle != null ? movieTitle : "Không xác định";
        String safeAuditorium = auditoriumName != null ? auditoriumName : "Không rõ phòng";
        String safeSeats = seatNames != null ? seatNames : "Không rõ ghế";
        String safeShowtime = showtime != null
                ? showtime.toString().replace("T", " ")
                : "Không rõ thời gian";


        BigDecimal safeCombo = comboTotal != null ? comboTotal : BigDecimal.ZERO;
        BigDecimal safeOriginal = originalPrice != null ? originalPrice : BigDecimal.ZERO;
        BigDecimal safeDiscount = discountTotal != null ? discountTotal : BigDecimal.ZERO;
        BigDecimal safeTotal = totalPrice != null ? totalPrice : BigDecimal.ZERO;
        String safeBranch = branchAddress != null ? branchAddress : "Không rõ địa chỉ";


        // 🔹 Tạo danh sách combo chi tiết
        String comboSection;
        if (comboDetails != null && !comboDetails.isEmpty()) {
            comboSection = "<h4 style='margin-top:20px;color:#0aa3ff;'> Combo đã chọn:</h4><ul style='padding-left:18px;'>";
            for (String c : comboDetails) {
                comboSection += "<li>" + c + "</li>";
            }
            comboSection += "</ul>";
        } else {
            comboSection = "<p style='color:#777;font-style:italic;'>Không có combo được chọn.</p>";
        }


        // 🧾 Nội dung email an toàn
        String content = """
   <div style="font-family: Arial, sans-serif; color:#222; line-height:1.6; max-width:600px; margin:auto;
               border:1px solid #ddd; border-radius:10px; overflow:hidden;">
     <div style="background:#0b162c; color:#fff; text-align:center; padding:20px;">
       <h2 style="margin:0;">🎟️ CineMaster</h2>
       <p>Vé xem phim của bạn đã được xác nhận!</p>
     </div>


     <div style="padding:20px;">
       <h3 style="text-align:center; color:#0b162c;">MÃ VÉ</h3>
       <h1 style="text-align:center; font-size:32px; color:#e50914;">%s</h1>


       <p style="text-align:center; font-size:18px; color:#0aa3ff; background:#f0f9ff; border:1px solid #0aa3ff;
                 display:inline-block; padding:8px 16px; border-radius:8px;">
         🔑 Mã xác minh OTP: <b>%s</b>
       </p>


       <div style="text-align:center; margin:20px 0;">
         <img src="%s" alt="QR Code" style="width:160px;height:160px;border:4px solid #0aa3ff;border-radius:12px;">
         <p style="font-size:13px; color:#555;">Quét mã QR này để xác thực vé tại rạp</p>
       </div>


       <table style="width:100%%; border-collapse:collapse; margin-top:10px;">
         <tr><td style="padding:8px 0; font-weight:bold;">🎬 Phim</td><td style="text-align:right;">%s</td></tr>
         <tr><td style="padding:8px 0; font-weight:bold;">🏠 Phòng chiếu</td><td style="text-align:right;">%s</td></tr>
         <tr><td style="padding:8px 0; font-weight:bold;">💺 Ghế</td><td style="text-align:right;">%s</td></tr>
         <tr><td style="padding:8px 0; font-weight:bold;">🕓 Suất chiếu</td><td style="text-align:right;">%s</td></tr>
         <tr><td style="padding:8px 0; font-weight:bold;">🥤 Tổng combo</td><td style="text-align:right;">%,.0f VND</td></tr>
         <tr><td style="padding:8px 0; font-weight:bold;">💰 Giá gốc</td><td style="text-align:right;">%,.0f VND</td></tr>
         <tr><td style="padding:8px 0; font-weight:bold;">🔻 Giảm giá</td><td style="text-align:right;color:#e50914;">-%,.0f VND</td></tr>
         <tr><td style="padding:8px 0; font-weight:bold;">✅ Thành tiền</td><td style="text-align:right;color:#0aa3ff;font-weight:bold;">%,.0f VND</td></tr>
       </table>


       %s <!-- 🟢 Combo chi tiết chèn ở đây -->


       <hr style="margin:20px 0; border:none; border-top:1px solid #ccc;">
       <p style="font-size:14px; color:#444;">📍 <b>Địa điểm:</b> %s</p>
       <p style="font-size:13px; color:#666;">Vui lòng cung cấp <b>OTP</b> hoặc <b>quét QR Code</b> để nhận vé tại quầy.</p>


       <div style="text-align:center; margin-top:20px;">
         <p style="color:#666; font-size:13px;">Cảm ơn bạn đã chọn <b>CineMaster</b>. Chúc bạn xem phim vui vẻ! 🍿</p>
       </div>
     </div>


     <div style="background:#f3f4f6; padding:15px; text-align:center; font-size:13px; color:#555;">
       <p>Hỗ trợ: <b>1900 1234</b> • Email: <a href="mailto:cs@cinemaster.vn">cs@cinemaster.vn</a></p>
     </div>
   </div>
   """.formatted(
                safeReservation,
                safeOtp,
                safeQr,
                safeMovie,
                safeAuditorium,
                safeSeats,
                safeShowtime,
                safeCombo,
                safeOriginal,
                safeDiscount,
                safeTotal,
                comboSection,
                safeBranch
        );


        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(content, true);


        try {
            mailSender.send(message);
            log.info("📩 Đã gửi email xác nhận vé cho {} (OTP={}, ComboTotal={})", to, safeOtp, safeCombo);
        } catch (Exception e) {
            e.printStackTrace();
            log.error(" Lỗi khi gửi email xác nhận vé cho {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Lỗi gửi email xác nhận vé", e);
        }




    }




}

