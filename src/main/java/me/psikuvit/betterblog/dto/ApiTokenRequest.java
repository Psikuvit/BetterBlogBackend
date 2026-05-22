package me.psikuvit.betterblog.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiTokenRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private List<String> scopes;

    private String expiresIn;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
class ApiTokenResponse {
    private Long id;
    private String name;
    private String token;
    private List<String> scopes;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;
}

