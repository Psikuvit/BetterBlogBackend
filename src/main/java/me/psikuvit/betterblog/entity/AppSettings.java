package me.psikuvit.betterblog.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "app_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppSettings {

    public static final String GLOBAL_ID = "global";

    @Id
    @Builder.Default
    private String id = GLOBAL_ID;

    @Builder.Default
    private int maxPostsPerUser = 50;

    private LocalDateTime updatedAt;

    private String updatedBy;
}
