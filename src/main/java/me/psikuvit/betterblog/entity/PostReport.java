package me.psikuvit.betterblog.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

import java.time.LocalDateTime;

@Document(collection = "post_reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostReport {

    @Id
    private String id;

    @DocumentReference
    private Post post;

    @DocumentReference
    private User reporter;

    private String reason;

    @Builder.Default
    private Status status = Status.PENDING;

    private LocalDateTime createdAt;

    private String resolvedBy;

    private LocalDateTime resolvedAt;

    public enum Status {
        PENDING, RESOLVED, DISMISSED
    }
}
