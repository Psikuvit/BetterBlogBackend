package me.psikuvit.betterblog.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.RequiredArgsConstructor;
import me.psikuvit.betterblog.exception.UnauthorizedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class JwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshTokenExpiration;

    private static final String ISSUER = "BetterBlog";

    /**
     * Generate access token for a user
     */
    public String generateAccessToken(String username) {
        return generateToken(username, jwtExpiration, "access");
    }

    /**
     * Generate refresh token for a user
     */
    public String generateRefreshToken(String username) {
        return generateToken(username, refreshTokenExpiration, "refresh");
    }

    /**
     * Generate JWT token with custom expiration
     */
    private String generateToken(String username, long expiration, String tokenType) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return JWT.create()
                .withIssuer(ISSUER)
                .withSubject(username)
                .withIssuedAt(now)
                .withExpiresAt(expiryDate)
                .withClaim("username", username)
                .withClaim("token_type", tokenType)
                .sign(Algorithm.HMAC512(jwtSecret));
    }

     /**
     * Validate and decode JWT token
     */
     public DecodedJWT validateToken(String token) {
         // Check if token is null or empty
         if (token == null || token.isBlank()) {
             throw new UnauthorizedException("Invalid or expired token: Token is empty");
         }

         // Check if token has the expected format (3 parts separated by dots)
         String[] parts = token.split("\\.");
         if (parts.length != 3) {
             throw new UnauthorizedException("Invalid or expired token: Token must have 3 parts");
         }

         try {
             return JWT.require(Algorithm.HMAC512(jwtSecret))
                     .withIssuer(ISSUER)
                     .build()
                     .verify(token);
         } catch (JWTVerificationException e) {
             throw new UnauthorizedException("Invalid or expired token: " + e.getMessage());
         }
     }

    /**
     * Validate and decode an access token (token_type = "access")
     */
    public DecodedJWT validateAccessToken(String token) {
        DecodedJWT decodedJWT = validateToken(token);
        String type = decodedJWT.getClaim("token_type").asString();
        if (!"access".equals(type)) {
            throw new UnauthorizedException("Invalid token type for access");
        }
        return decodedJWT;
    }

    /**
     * Validate and decode a refresh token (token_type = "refresh")
     */
    public DecodedJWT validateRefreshToken(String token) {
        DecodedJWT decodedJWT = validateToken(token);
        String type = decodedJWT.getClaim("token_type").asString();
        if (!"refresh".equals(type)) {
            throw new UnauthorizedException("Invalid token type for refresh");
        }
        return decodedJWT;
    }

    /**
     * Extract username from token
     */
    public String getUsernameFromToken(String token) {
        DecodedJWT decodedJWT = validateToken(token);
        return decodedJWT.getSubject();
    }

    /**
     * Check if token is expired
     */
    public boolean isTokenExpired(String token) {
        try {
            DecodedJWT decodedJWT = JWT.decode(token);
            return decodedJWT.getExpiresAt().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Get expiration time from token
     */
    public long getExpirationTimeFromToken(String token) {
        DecodedJWT decodedJWT = validateToken(token);
        return decodedJWT.getExpiresAt().getTime();
    }

    /**
     * Get remaining expiration in milliseconds
     */
    public long getRemainingExpirationTimeMs(String token) {
        try {
            DecodedJWT decodedJWT = JWT.decode(token);
            long expirationTime = decodedJWT.getExpiresAt().getTime();
            long currentTime = System.currentTimeMillis();
            return Math.max(0, expirationTime - currentTime);
        } catch (Exception e) {
            return 0;
        }
    }
}

