package me.psikuvit.betterblog.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.psikuvit.betterblog.config.RateLimiterService;
import me.psikuvit.betterblog.dto.AuthResponse;
import me.psikuvit.betterblog.dto.LoginRequest;
import me.psikuvit.betterblog.dto.RegisterRequest;
import me.psikuvit.betterblog.dto.UserDto;
import me.psikuvit.betterblog.exception.RateLimitExceededException;
import me.psikuvit.betterblog.service.AuthService;
import me.psikuvit.betterblog.service.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080"})
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final RateLimiterService rateLimiterService;

    /**
     * Register a new user
     * POST /auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {
        String clientIp = getClientIp(httpRequest);
        
        // Rate limiting: 3 registrations per hour per IP
        if (!rateLimiterService.allowRegistration(clientIp)) {
            throw new RateLimitExceededException(
                    "Too many registration attempts. Please try again later.", 3600);
        }
        
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Login user with credentials
     * POST /auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        String clientIp = getClientIp(httpRequest);
        
        // Rate limiting: 10 login attempts per minute per IP
        if (!rateLimiterService.allowLoginAttempt(clientIp)) {
            throw new RateLimitExceededException(
                    "Too many login attempts. Please try again later.", 60);
        }
        
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Refresh access token using refresh token
     * POST /auth/refresh
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().build();
        }

        String refreshToken = authHeader.substring("Bearer ".length());
        AuthResponse response = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(response);
    }

    /**
     * Get current authenticated user details
     * GET /auth/me
     */
    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String username = authentication.getName();
        UserDto userDto = authService.getUserByUsername(username);
        return ResponseEntity.ok(userDto);
    }

    /**
     * Logout user (client-side should discard tokens)
     * POST /auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok().build();
    }

    /**
     * Validate token endpoint
     * GET /auth/validate
     */
    @GetMapping("/validate")
    public ResponseEntity<Boolean> validateToken(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.ok(false);
        }

        try {
            String token = authHeader.substring("Bearer ".length());
            jwtService.validateToken(token);
            return ResponseEntity.ok(true);
        } catch (Exception e) {
            return ResponseEntity.ok(false);
        }
    }

    /**
     * Extract client IP address from request
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}

