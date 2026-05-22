package me.psikuvit.betterblog.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {

    private String username;

    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    /**
     * At least one of username or email must be provided
     */
    public boolean isValid() {
        return (username != null && !username.isBlank()) || 
               (email != null && !email.isBlank());
    }
}

