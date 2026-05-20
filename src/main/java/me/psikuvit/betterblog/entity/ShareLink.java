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

@Document(collection = "share_links")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShareLink {

    @Id
    private String id;

    @DocumentReference
    private Post post;

    @DocumentReference
    private User user;

    @Indexed(unique = true)
    private String token;

    private LocalDateTime expiresAt;

    @Builder.Default
    private Integer accessCount = 0;

    private Integer maxAccess;

    private LocalDateTime createdAt;
}

