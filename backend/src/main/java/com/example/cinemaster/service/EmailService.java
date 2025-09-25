package com.example.cinemaster.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

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

}
