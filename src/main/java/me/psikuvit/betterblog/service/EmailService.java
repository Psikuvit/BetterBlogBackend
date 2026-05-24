package me.psikuvit.betterblog.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Value("${app.resend.api-key:}")
    private String resendApiKey;

    @Value("${app.mail.from:no-reply@betterblog.com}")
    private String fromAddress;

    public void sendPasswordResetCode(String to, String code, int expiresInMinutes) {
        String subject = "Your BetterBlog password reset code";
        String text = buildPasswordResetMessage(code, expiresInMinutes);
        sendEmail(to, subject, text);
    }

    public void sendTestEmail(String to) {
        sendEmail(
                to,
                "BetterBlog mail test",
                "This is a test email from BetterBlog. If you received this, Resend is working."
        );
    }

    private void sendEmail(String to, String subject, String text) {
        if (resendApiKey == null || resendApiKey.isBlank()) {
            throw new IllegalStateException("Resend API key is not configured");
        }

        try {
            Resend resend = new Resend(resendApiKey);
            CreateEmailOptions options = CreateEmailOptions.builder()
                    .from(fromAddress)
                    .to(to)
                    .subject(subject)
                    .text(text)
                    .build();

            String emailId = resend.emails().send(options).getId();
            log.debug("Resend accepted email to {} with id {}", to, emailId);
        } catch (ResendException e) {
            throw new IllegalStateException("Resend request failed: " + e.getMessage(), e);
        }
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

