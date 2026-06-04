package me.psikuvit.betterblog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppSettingsDto {

    private int maxPostsPerUser;
    private LocalDateTime updatedAt;
    private String updatedBy;
}
