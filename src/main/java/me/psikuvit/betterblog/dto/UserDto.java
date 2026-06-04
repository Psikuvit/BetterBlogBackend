package me.psikuvit.betterblog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.psikuvit.betterblog.entity.User;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {

    private String id;
    private String username;
    private String email;
    private String profilePictureUrl;
    private User.Role role;
    private String bio;
    private boolean enabled;
}

