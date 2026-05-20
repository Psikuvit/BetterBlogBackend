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
public class PostRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Slug is required")
    private String slug;

    private String excerpt;

    @NotBlank(message = "Content is required")
    private String content;

    private List<String> tags;

    @Builder.Default
    private String visibility = "PUBLIC";

    private String coverImageUrl;

    private String sourceUrl;

    private String sourcePreviewTitle;

    private String sourcePreviewDescription;

    private String sourcePreviewImage;

    private String originalAuthor;

    private String legacyId;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
class PostResponse {
    private Long id;
    private String title;
    private String slug;
    private String excerpt;
    private String content;
    private List<String> tags;
    private String visibility;
    private String coverImageUrl;
    private boolean isPublic;
    private LocalDateTime publishedAt;
    private String sourceUrl;
    private String sourcePreviewTitle;
    private String sourcePreviewDescription;
    private String sourcePreviewImage;
    private String originalAuthor;
    private String legacyId;
    private LocalDateTime importedAt;
    private Long authorId;
    private String authorUsername;
    private String madePrivateBy;
    private LocalDateTime madePrivateAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


