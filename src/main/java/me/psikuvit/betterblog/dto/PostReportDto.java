package me.psikuvit.betterblog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.psikuvit.betterblog.entity.Post;
import me.psikuvit.betterblog.entity.PostReport;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostReportDto {

    private String id;
    private String postId;
    private String postTitle;
    private String reporterUsername;
    private String reason;
    private String status;
    private LocalDateTime createdAt;
    private String resolvedBy;
    private LocalDateTime resolvedAt;

    public static PostReportDto from(PostReport report) {
        Post post = report.getPost();
        return PostReportDto.builder()
                .id(report.getId())
                .postId(post != null ? post.getId() : null)
                .postTitle(post != null ? post.getTitle() : null)
                .reporterUsername(report.getReporter() != null ? report.getReporter().getUsername() : null)
                .reason(report.getReason())
                .status(report.getStatus() != null ? report.getStatus().name() : null)
                .createdAt(report.getCreatedAt())
                .resolvedBy(report.getResolvedBy())
                .resolvedAt(report.getResolvedAt())
                .build();
    }
}
