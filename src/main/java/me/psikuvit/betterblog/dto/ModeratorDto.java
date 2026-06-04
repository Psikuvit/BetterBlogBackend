package me.psikuvit.betterblog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.psikuvit.betterblog.entity.User;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModeratorDto {

    private String id;
    private String username;
    private String email;
    private List<String> permissions;
    private LocalDateTime assignedAt;
    private String assignedBy;

    public static ModeratorDto fromUser(User user) {
        List<String> permissions = user.getModeratorPermissions() != null
                ? user.getModeratorPermissions()
                : Collections.emptyList();

        LocalDateTime assignedAt = user.getModeratorAssignedAt();
        if (assignedAt == null) {
            assignedAt = user.getUpdatedAt() != null ? user.getUpdatedAt() : user.getCreatedAt();
        }

        String assignedBy = user.getModeratorAssignedBy() != null ? user.getModeratorAssignedBy() : "";

        return ModeratorDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .permissions(permissions)
                .assignedAt(assignedAt)
                .assignedBy(assignedBy)
                .build();
    }
}
