package com.pharmacy.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    // IP -> Request count mapping
    private final Map<String, RateLimitInfo> requestCounts = new ConcurrentHashMap<>();

    // General rate limit: 100 requests per minute
    private static final int GENERAL_LIMIT = 100;
    private static final long GENERAL_WINDOW_MS = 60 * 1000; // 1 minute

    // Login rate limit: 5 attempts per minute (stricter)
    private static final int LOGIN_LIMIT = 5;
    private static final long LOGIN_WINDOW_MS = 60 * 1000; // 1 minute

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String clientIP = getClientIP(request);
        String path = request.getRequestURI();

        // Determine which limit to apply
        boolean isLoginRequest = path.contains("/api/auth/login");
        int limit = isLoginRequest ? LOGIN_LIMIT : GENERAL_LIMIT;
        long windowMs = isLoginRequest ? LOGIN_WINDOW_MS : GENERAL_WINDOW_MS;
        String key = clientIP + (isLoginRequest ? ":login" : ":general");

        // Check rate limit
        RateLimitInfo info = requestCounts.compute(key, (k, v) -> {
            long now = System.currentTimeMillis();
            if (v == null || now - v.windowStart > windowMs) {
                return new RateLimitInfo(now, 1);
            }
            v.count.incrementAndGet();
            return v;
        });

        // Add rate limit headers
        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, limit - info.count.get())));
        response.setHeader("X-RateLimit-Reset", String.valueOf((info.windowStart + windowMs) / 1000));

        // Check if limit exceeded
        if (info.count.get() > limit) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\":\"Too many requests\",\"message\":\"Rate limit exceeded. Please try again later.\",\"retryAfter\":" +
                            ((info.windowStart + windowMs - System.currentTimeMillis()) / 1000) + "}"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIP(HttpServletRequest request) {
        // Check for proxy headers
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            return ip.split(",")[0].trim();
        }

        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        return request.getRemoteAddr();
    }

    // Inner class to hold rate limit info
    private static class RateLimitInfo {
        long windowStart;
        AtomicInteger count;

        RateLimitInfo(long windowStart, int count) {
            this.windowStart = windowStart;
            this.count = new AtomicInteger(count);
        }
    }

    // Cleanup old entries periodically (call this from a scheduled task)
    public void cleanup() {
        long now = System.currentTimeMillis();
        requestCounts.entrySet().removeIf(entry ->
                now - entry.getValue().windowStart > GENERAL_WINDOW_MS * 2
        );
    }
}
