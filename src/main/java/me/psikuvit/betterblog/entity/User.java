package me.psikuvit.betterblog.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @JsonIgnore
    private String password;

    @JsonIgnore
    private String passwordResetCodeHash;

    @JsonIgnore
    private LocalDateTime passwordResetCodeExpiresAt;

    private String profilePictureUrl;

    private String bio;

    @Builder.Default
    private boolean enabled = true;

    @Builder.Default
    private Role role = Role.USER;

    @Builder.Default
    private List<String> moderatorPermissions = new ArrayList<>();

    private LocalDateTime moderatorAssignedAt;

    private String moderatorAssignedBy;

    private String preferences;

    private LocalDateTime lastLoginAt;

    public enum Role {
        USER, MODERATOR, ADMIN
    }

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
