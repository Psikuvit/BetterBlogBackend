package me.psikuvit.betterblog.service;

import lombok.RequiredArgsConstructor;
import me.psikuvit.betterblog.dto.AuthResponse;
import me.psikuvit.betterblog.dto.LoginRequest;
import me.psikuvit.betterblog.dto.RegisterRequest;
import me.psikuvit.betterblog.dto.UserDto;
import com.auth0.jwt.interfaces.DecodedJWT;
import me.psikuvit.betterblog.entity.User;
import me.psikuvit.betterblog.exception.AlreadyExistsException;
import me.psikuvit.betterblog.exception.BadRequestException;
import me.psikuvit.betterblog.exception.ResourceNotFoundException;
import me.psikuvit.betterblog.exception.UnauthorizedException;
import me.psikuvit.betterblog.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;

    /**
     * Register a new user
     */
    public AuthResponse register(RegisterRequest request) {
        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AlreadyExistsException("Username already exists: " + request.getUsername());
        }

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AlreadyExistsException("Email already exists: " + request.getEmail());
        }

        // Create new user
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .enabled(true)
                .build();

        user = userRepository.save(user);

        // Generate tokens
        String accessToken = jwtService.generateAccessToken(user.getUsername());
        String refreshToken = jwtService.generateRefreshToken(user.getUsername());

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    /**
     * Login user with username or email and password
     */
    public AuthResponse login(LoginRequest request) {
        // Validate that at least username or email is provided
        if (!request.isValid()) {
            throw new BadRequestException("Either username or email is required");
        }

        // Try to find user by username or email
        User user = null;
        String identifier = "";

        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            user = userRepository.findByUsername(request.getUsername()).orElse(null);
            identifier = request.getUsername();
        }

        if (user == null && request.getEmail() != null && !request.getEmail().isBlank()) {
            user = userRepository.findByEmail(request.getEmail()).orElse(null);
            identifier = request.getEmail();
        }

        if (user == null) {
            throw new ResourceNotFoundException("User not found: " + (identifier.isEmpty() ? "invalid credentials" : identifier));
        }

        if (!user.isEnabled()) {
            throw new UnauthorizedException("User account is disabled");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid password");
        }

        // Generate tokens
        String accessToken = jwtService.generateAccessToken(user.getUsername());
        String refreshToken = jwtService.generateRefreshToken(user.getUsername());

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    /**
     * Refresh access token using refresh token
     */
    public AuthResponse refreshToken(String refreshToken) {
        // Reject revoked refresh tokens
        if (tokenBlacklistService.isRevoked(refreshToken)) {
            throw new UnauthorizedException("Refresh token has been revoked");
        }

        DecodedJWT decoded = jwtService.validateRefreshToken(refreshToken);
        String username = decoded.getSubject();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        if (!user.isEnabled()) {
            throw new UnauthorizedException("User account is disabled");
        }

        // Revoke the used refresh token
        tokenBlacklistService.revokeToken(refreshToken);

        // Generate new access token and refresh token
        String newAccessToken = jwtService.generateAccessToken(user.getUsername());
        String newRefreshToken = jwtService.generateRefreshToken(user.getUsername());

        return buildAuthResponse(user, newAccessToken, newRefreshToken);
    }

    /**
     * Get user by username
     */
    public UserDto getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        return mapToUserDto(user);
    }

    /**
     * Get the full user entity by username.
     */
    public User getUserEntityByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    /**
     * Build AuthResponse with tokens and user info
     */
    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        long expiresIn = 86400; // 24 hours in seconds

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .user(mapToUserDto(user))
                .build();
    }

    /**
     * Map User entity to UserDto
     */
    private UserDto mapToUserDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .profilePictureUrl(user.getProfilePictureUrl())
                .bio(user.getBio())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .build();
    }
}


