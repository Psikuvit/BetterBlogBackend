package me.psikuvit.betterblog.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;
import java.time.LocalDateTime;

@Document(collection = "activity_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityLog {

    @Id
    private String id;

    @DocumentReference
    private User user;

    private String action;

    private String resourceType;

    private String resourceId;

    private String resourceName;

    private String details;

    private String ipAddress;

    private String userAgent;

    @Builder.Default
    private Severity severity = Severity.INFO;

    private LocalDateTime createdAt;


    public enum Severity {
        INFO, WARNING, CRITICAL
    }
}

