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
public class ShareLinkRequest {
    private String postId;
    private String expiresIn;
    private Integer maxAccess;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
class ShareLinkResponse {
    private String id;
    private String postId;
    private String token;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private Integer accessCount;
    private Integer maxAccess;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
class ShareLinkAccessResponse {
    private Object post;
}

