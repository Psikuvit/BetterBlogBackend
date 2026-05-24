package me.psikuvit.betterblog.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final ObjectProvider<JavaMailSender> javaMailSenderProvider;

    @Value("${app.mail.from:no-reply@betterblog.local}")
    private String fromAddress;

    public void sendPasswordResetCode(String to, String code, int expiresInMinutes) {
        JavaMailSender mailSender = javaMailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            throw new IllegalStateException("Mail sender is not configured");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setFrom(fromAddress);
        message.setSubject("Your BetterBlog password reset code");
        message.setText(buildPasswordResetMessage(code, expiresInMinutes));

        mailSender.send(message);
    }

    private String buildPasswordResetMessage(String code, int expiresInMinutes) {
        return String.format(
                "We received a request to reset your BetterBlog password.%n%n" +
                "Your password reset code is: %s%n%n" +
                "This code will expire in %d minutes.%n%n" +
                "If you did not request this, you can safely ignore this email.",
                code,
                expiresInMinutes
        );
    }
}

