package me.psikuvit.betterblog.service;

import lombok.RequiredArgsConstructor;
import me.psikuvit.betterblog.dto.ForgotPasswordResponse;
import me.psikuvit.betterblog.dto.ResetPasswordResponse;
import me.psikuvit.betterblog.entity.User;
import me.psikuvit.betterblog.exception.BadRequestException;
import me.psikuvit.betterblog.exception.ResourceNotFoundException;
import me.psikuvit.betterblog.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.CompletableFuture;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@Transactional
@RequiredArgsConstructor
public class PasswordResetService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.password-reset.code-validity-minutes:30}")
    private int codeValidityMinutes;

    public ForgotPasswordResponse requestPasswordReset(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            String code = generateSixDigitCode();
            user.setPasswordResetCodeHash(passwordEncoder.encode(code));
            user.setPasswordResetCodeExpiresAt(LocalDateTime.now().plusMinutes(codeValidityMinutes));
            userRepository.save(user);

            CompletableFuture.runAsync(() -> {
                try {
                    emailService.sendPasswordResetCode(user.getEmail(), code, codeValidityMinutes);
                } catch (Exception e) {
                    // Log and swallow exceptions to avoid leaking SMTP errors to clients
                    log.warn("Failed to send password reset email to {}", user.getEmail(), e);
                }
            });
        });

        return ForgotPasswordResponse.builder()
                .message("If an account with that email exists, a reset code has been sent.")
                .expiresInMinutes(codeValidityMinutes)
                .build();
    }

    @Transactional(noRollbackFor = BadRequestException.class)
    public ResetPasswordResponse resetPassword(String email, String code, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));

        if (user.getPasswordResetCodeHash() == null || user.getPasswordResetCodeExpiresAt() == null) {
            throw new BadRequestException("Reset code is missing or expired");
        }

        if (LocalDateTime.now().isAfter(user.getPasswordResetCodeExpiresAt())) {
            clearResetCode(user);
            userRepository.save(user);
            throw new BadRequestException("Reset code has expired");
        }

        if (!passwordEncoder.matches(code, user.getPasswordResetCodeHash())) {
            throw new BadRequestException("Invalid reset code");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        clearResetCode(user);
        userRepository.save(user);

        return ResetPasswordResponse.builder()
                .message("Password reset successful")
                .build();
    }

    private void clearResetCode(User user) {
        user.setPasswordResetCodeHash(null);
        user.setPasswordResetCodeExpiresAt(null);
    }

    private String generateSixDigitCode() {
        int code = 100000 + SECURE_RANDOM.nextInt(900000);
        return String.valueOf(code);
    }
}



