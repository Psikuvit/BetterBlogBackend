package me.psikuvit.betterblog.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private String username;

    @Indexed(unique = true)
    private String email;

    private String password;

    private String passwordResetCodeHash;

    private LocalDateTime passwordResetCodeExpiresAt;

    private String profilePictureUrl;

    private String bio;

    @Builder.Default
    private boolean enabled = true;

    @Builder.Default
    private Role role = Role.USER;

    private String preferences;

    private LocalDateTime lastLoginAt;

    public enum Role {
        USER, MODERATOR, ADMIN
    }

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}


