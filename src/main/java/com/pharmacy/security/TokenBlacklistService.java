package com.pharmacy.security;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenBlacklistService {

    // Token -> Expiration time mapping
    private final Map<String, Long> blacklistedTokens = new ConcurrentHashMap<>();

    // Add token to blacklist
    public void blacklistToken(String token, Date expirationDate) {
        blacklistedTokens.put(token, expirationDate.getTime());
    }

    // Check if token is blacklisted
    public boolean isBlacklisted(String token) {
        return blacklistedTokens.containsKey(token);
    }

    // Remove token from blacklist (manual cleanup if needed)
    public void removeFromBlacklist(String token) {
        blacklistedTokens.remove(token);
    }

    // Cleanup expired tokens every hour
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void cleanupExpiredTokens() {
        long now = System.currentTimeMillis();
        blacklistedTokens.entrySet().removeIf(entry -> entry.getValue() < now);
    }

    // Get blacklist size (for monitoring)
    public int getBlacklistSize() {
        return blacklistedTokens.size();
    }

    // Clear all blacklisted tokens (emergency use only)
    public void clearBlacklist() {
        blacklistedTokens.clear();
    }
}
