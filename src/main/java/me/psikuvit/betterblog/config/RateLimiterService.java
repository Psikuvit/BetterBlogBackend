package me.psikuvit.betterblog.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class RateLimiterService {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    // Rate limit: 10 requests per minute per endpoint for login attempts
    private static final int LOGIN_REQUESTS_PER_MINUTE = 10;

    // Rate limit: 3 requests per hour per IP for registration
    private static final int REGISTER_REQUESTS_PER_HOUR = 3;

    // Rate limit: 5 requests per hour per IP for password reset requests
    private static final int PASSWORD_RESET_REQUESTS_PER_HOUR = 5;

    // Rate limit: 60 refreshes per hour per IP
    private static final int REFRESH_REQUESTS_PER_HOUR = 60;

    /**
     * Get or create bucket for login rate limiting (10 req/min)
     */
    public Bucket getLoginBucket(String key) {
        return cache.computeIfAbsent("login_" + key, k -> createLoginBucket());
    }

    /**
     * Get or create bucket for registration rate limiting (3 req/hour)
     */
    public Bucket getRegisterBucket(String key) {
        return cache.computeIfAbsent("register_" + key, k -> createRegisterBucket());
    }

    /**
     * Get or create bucket for password reset requests (5 req/hour)
     */
    public Bucket getPasswordResetBucket(String key) {
        return cache.computeIfAbsent("password_reset_" + key, k -> createPasswordResetBucket());
    }

    /**
     * Check if login attempt is allowed and consume a token
     */
    public boolean allowLoginAttempt(String key) {
        return getLoginBucket(key).tryConsume(1);
    }

    /**
     * Check if registration is allowed and consume a token
     */
    public boolean allowRegistration(String key) {
        return getRegisterBucket(key).tryConsume(1);
    }

    /**
     * Check if password reset is allowed and consume a token
     */
    public boolean allowPasswordResetRequest(String key) {
        return getPasswordResetBucket(key).tryConsume(1);
    }

    /**
     * Get or create bucket for refresh token usage (60 req/hour)
     */
    public Bucket getRefreshBucket(String key) {
        return cache.computeIfAbsent("refresh_" + key, k -> createRefreshBucket());
    }

    /**
     * Check if refresh is allowed and consume a token
     */
    public boolean allowRefresh(String key) {
        return getRefreshBucket(key).tryConsume(1);
    }

    private Bucket createLoginBucket() {
        Bandwidth limit = Bandwidth.classic(LOGIN_REQUESTS_PER_MINUTE, Refill.intervally(LOGIN_REQUESTS_PER_MINUTE, Duration.ofMinutes(1)));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Create register bucket: 3 requests per hour
     */
    private Bucket createRegisterBucket() {
        Bandwidth limit = Bandwidth.classic(REGISTER_REQUESTS_PER_HOUR, Refill.intervally(REGISTER_REQUESTS_PER_HOUR, Duration.ofHours(1)));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Create password reset bucket: 5 requests per hour
     */
    private Bucket createPasswordResetBucket() {
        Bandwidth limit = Bandwidth.classic(PASSWORD_RESET_REQUESTS_PER_HOUR, Refill.intervally(PASSWORD_RESET_REQUESTS_PER_HOUR, Duration.ofHours(1)));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Create refresh bucket: 60 requests per hour
     */
    private Bucket createRefreshBucket() {
        Bandwidth limit = Bandwidth.classic(REFRESH_REQUESTS_PER_HOUR, Refill.intervally(REFRESH_REQUESTS_PER_HOUR, Duration.ofHours(1)));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}

