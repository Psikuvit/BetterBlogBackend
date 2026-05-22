package me.psikuvit.betterblog.service;

import lombok.RequiredArgsConstructor;
import me.psikuvit.betterblog.dto.ApiTokenRequest;
import me.psikuvit.betterblog.entity.ApiToken;
import me.psikuvit.betterblog.entity.User;
import me.psikuvit.betterblog.exception.ResourceNotFoundException;
import me.psikuvit.betterblog.exception.BadRequestException;
import me.psikuvit.betterblog.repository.ApiTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class ApiTokenService {

    private final ApiTokenRepository apiTokenRepository;
    private final ActivityLogService activityLogService;

    public ApiToken createApiToken(ApiTokenRequest request, User user) {
        String token = "bb_" + UUID.randomUUID().toString();
        LocalDateTime expiresAt = calculateExpiry(request.getExpiresIn());

        ApiToken apiToken = ApiToken.builder()
                .user(user)
                .name(request.getName())
                .token(token)
                .scopes(request.getScopes())
                .expiresAt(expiresAt)
                .build();

        apiToken = apiTokenRepository.save(apiToken);
        activityLogService.logActivity(user, "API_TOKEN_CREATED", "ApiToken", apiToken.getId().toString(), request.getName());
        return apiToken;
    }

    public void revokeApiToken(String tokenId, User user) {
        ApiToken token = apiTokenRepository.findById(tokenId)
                .orElseThrow(() -> new ResourceNotFoundException("Token not found"));

        if (!token.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You can only revoke your own tokens");
        }

        apiTokenRepository.delete(token);
        activityLogService.logActivity(user, "API_TOKEN_REVOKED", "ApiToken", tokenId.toString(), token.getName());
    }

    public List<ApiToken> getUserTokens(User user) {
        return apiTokenRepository.findByUser(user);
    }

    public ApiToken validateAndGetToken(String token) {
        ApiToken apiToken = apiTokenRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid token"));

        if (apiToken.getExpiresAt() != null && apiToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResourceNotFoundException("Token has expired");
        }

        apiToken.setLastUsedAt(LocalDateTime.now());
        apiTokenRepository.save(apiToken);

        return apiToken;
    }

    private LocalDateTime calculateExpiry(String expiresIn) {
        return switch (expiresIn) {
            case "7d" -> LocalDateTime.now().plusDays(7);
            case "30d" -> LocalDateTime.now().plusDays(30);
            case "90d" -> LocalDateTime.now().plusDays(90);
            case "1y" -> LocalDateTime.now().plusYears(1);
            default -> LocalDateTime.now().plusDays(30);
        };
    }
}

