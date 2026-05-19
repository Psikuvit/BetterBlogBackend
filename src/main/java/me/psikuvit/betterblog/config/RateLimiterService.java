package me.psikuvit.betterblog.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
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

    // Rate limit: 5 requests per minute per IP
    private static final int REQUESTS_PER_MINUTE = 5;

    // Rate limit: 10 requests per minute per endpoint for login attempts
    private static final int LOGIN_REQUESTS_PER_MINUTE = 10;

    // Rate limit: 3 requests per hour per IP for registration
    private static final int REGISTER_REQUESTS_PER_HOUR = 3;

    /**
     * Get or create bucket for general rate limiting (5 req/min)
     */
    public Bucket getBucket(String key) {
        return cache.computeIfAbsent(key, k -> createStandardBucket());
    }

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
     * Check if request is allowed and consume a token
     */
    public boolean allowRequest(String key) {
        return getBucket(key).tryConsume(1);
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
     * Get remaining tokens for a key
     */
    public long getRemainingTokens(String key) {
        return getBucket(key).getAvailableTokens();
    }

    /**
     * Get remaining tokens for login bucket
     */
    public long getRemainingLoginTokens(String key) {
        return getLoginBucket(key).getAvailableTokens();
    }

    /**
     * Get remaining tokens for register bucket
     */
    public long getRemainingRegisterTokens(String key) {
        return getRegisterBucket(key).getAvailableTokens();
    }

    /**
     * Create standard bucket: 5 requests per minute
     */
    private Bucket createStandardBucket() {
        Bandwidth limit = Bandwidth.classic(REQUESTS_PER_MINUTE, Refill.intervally(REQUESTS_PER_MINUTE, Duration.ofMinutes(1)));
        return Bucket4j.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Create login bucket: 10 requests per minute
     */
    private Bucket createLoginBucket() {
        Bandwidth limit = Bandwidth.classic(LOGIN_REQUESTS_PER_MINUTE, Refill.intervally(LOGIN_REQUESTS_PER_MINUTE, Duration.ofMinutes(1)));
        return Bucket4j.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Create register bucket: 3 requests per hour
     */
    private Bucket createRegisterBucket() {
        Bandwidth limit = Bandwidth.classic(REGISTER_REQUESTS_PER_HOUR, Refill.intervally(REGISTER_REQUESTS_PER_HOUR, Duration.ofHours(1)));
        return Bucket4j.builder()
                .addLimit(limit)
                .build();
    }
}

