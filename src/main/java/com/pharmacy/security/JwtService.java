package com.pharmacy.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);
    private static final int MINIMUM_SECRET_LENGTH = 32;

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    /**
     * Validates JWT configuration on application startup.
     * Ensures JWT_SECRET environment variable is properly set.
     *
     * @throws IllegalStateException if JWT secret is missing or invalid
     */
    @PostConstruct
    public void validateConfiguration() {
        logger.info("Validating JWT configuration...");

        // Check if secret key is null or empty
        if (secretKey == null || secretKey.trim().isEmpty()) {
            throw new IllegalStateException(
                    "SECURITY ERROR: JWT_SECRET environment variable is required but not set. " +
                            "Please set JWT_SECRET in your environment variables or .env file. " +
                            "Example: JWT_SECRET=your-base64-encoded-secret-key-here"
            );
        }

        // Check minimum length
        if (secretKey.length() < MINIMUM_SECRET_LENGTH) {
            throw new IllegalStateException(
                    String.format(
                            "SECURITY ERROR: JWT_SECRET is too short (%d characters). " +
                                    "Minimum required length is %d characters for secure token signing. " +
                                    "Please generate a longer secret key.",
                            secretKey.length(),
                            MINIMUM_SECRET_LENGTH
                    )
            );
        }

        // Validate Base64 encoding
        try {
            Decoders.BASE64.decode(secretKey);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "SECURITY ERROR: JWT_SECRET must be a valid Base64-encoded string. " +
                            "Current value is not valid Base64. " +
                            "Please generate a proper secret using: openssl rand -base64 64",
                    e
            );
        }

        // Log success (without exposing the secret)
        logger.info("✓ JWT configuration validated successfully");
        logger.info("✓ JWT Access Token Expiration: {} ms ({} hours)",
                jwtExpiration, jwtExpiration / 3600000);
        logger.info("✓ JWT Refresh Token Expiration: {} ms ({} days)",
                refreshExpiration, refreshExpiration / 86400000);
    }

    // Extract username from token
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Extract single claim from token
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // Generate token with only username
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    // Generate token with extra claims
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return buildToken(extraClaims, userDetails, jwtExpiration);
    }

    // Generate refresh token
    public String generateRefreshToken(UserDetails userDetails) {
        return buildToken(new HashMap<>(), userDetails, refreshExpiration);
    }

    // Build JWT token (jjwt 0.12.x syntax)
    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long expiration) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey())
                .compact();
    }

    // Validate token
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    // Check if token is expired
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // Extract expiration date from token
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // Extract all claims from token (jjwt 0.12.x syntax)
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // Get signing key from secret
    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // Get expiration time in milliseconds
    public long getExpirationTime() {
        return jwtExpiration;
    }
}