package me.psikuvit.betterblog.dto;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateAppSettingsRequest {

    @Min(value = 1, message = "maxPostsPerUser must be at least 1")
    private Integer maxPostsPerUser;
}
