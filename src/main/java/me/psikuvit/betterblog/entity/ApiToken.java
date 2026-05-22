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

@Document(collection = "api_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiToken {

    @Id
    private String id;

    @DocumentReference
    private User user;

    private String name;

    @Indexed(unique = true)
    private String token;

    private List<String> scopes;

    private LocalDateTime expiresAt;

    private LocalDateTime lastUsedAt;

    private LocalDateTime createdAt;
}

