package me.psikuvit.betterblog.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory token blacklist for revoked refresh tokens.
 * Tokens are stored with their expiry time and cleaned up lazily.
 * This is intentionally lightweight; for production consider a persisted store.
 */
@Service
public class TokenBlacklistService {

    private final Map<String, Long> blacklist = new ConcurrentHashMap<>();

    public void revokeToken(String token) {
        if (token == null || token.isBlank()) return;
        long expiry = Instant.now().plusSeconds(7 * 24 * 3600).toEpochMilli();
        blacklist.put(token, expiry);
    }

    public boolean isRevoked(String token) {
        if (token == null || token.isBlank()) return false;
        Long expiry = blacklist.get(token);
        if (expiry == null) return false;
        if (Instant.now().toEpochMilli() > expiry) {
            blacklist.remove(token);
            return false;
        }
        return true;
    }
}

