package me.psikuvit.betterblog.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;
import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "posts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post {

    @Id
    private String id;

    private String title;

    @Indexed(unique = true)
    private String slug;

    private String excerpt;

    private String content;

    private List<String> tags;

    @Builder.Default
    private Visibility visibility = Visibility.PUBLIC;

    private String coverImageUrl;

    @Builder.Default
    private boolean isPublic = true;

    private LocalDateTime publishedAt;

    private String sourceUrl;

    private String sourcePreviewTitle;

    private String sourcePreviewDescription;

    private String sourcePreviewImage;

    private String originalAuthor;

    private String legacyId;

    private LocalDateTime importedAt;

    @DocumentReference
    private User author;

    private String madePrivateBy;

    private LocalDateTime madePrivateAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;


    public enum Visibility {
        PUBLIC, PRIVATE, ADMIN_PRIVATE
    }
}


