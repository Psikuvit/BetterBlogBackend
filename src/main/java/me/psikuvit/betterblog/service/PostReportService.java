package me.psikuvit.betterblog.service;

import lombok.RequiredArgsConstructor;
import me.psikuvit.betterblog.dto.PostReportDto;
import me.psikuvit.betterblog.dto.PostReportRequest;
import me.psikuvit.betterblog.dto.ResolveReportRequest;
import me.psikuvit.betterblog.entity.Post;
import me.psikuvit.betterblog.entity.PostReport;
import me.psikuvit.betterblog.entity.User;
import me.psikuvit.betterblog.exception.BadRequestException;
import me.psikuvit.betterblog.exception.ForbiddenException;
import me.psikuvit.betterblog.exception.ResourceNotFoundException;
import me.psikuvit.betterblog.repository.PostReportRepository;
import me.psikuvit.betterblog.repository.PostRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
@Transactional
@RequiredArgsConstructor
public class PostReportService {

    private final PostReportRepository postReportRepository;
    private final PostRepository postRepository;

    public PostReportDto reportPost(String postId, PostReportRequest request, User reporter) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        if (post.getVisibility() != Post.Visibility.PUBLIC) {
            throw new BadRequestException("Only public posts can be reported");
        }

        PostReport report = PostReport.builder()
                .post(post)
                .reporter(reporter)
                .reason(request.getReason().trim())
                .status(PostReport.Status.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        return PostReportDto.from(postReportRepository.save(report));
    }

    public Page<PostReportDto> getReports(String status, Pageable pageable) {
        if (status == null || status.isBlank()) {
            return postReportRepository.findAll(pageable).map(PostReportDto::from);
        }

        PostReport.Status reportStatus;
        try {
            reportStatus = PostReport.Status.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid status value: " + status);
        }

        return postReportRepository.findByStatus(reportStatus, pageable).map(PostReportDto::from);
    }

    public PostReportDto resolveReport(String reportId, ResolveReportRequest request, User moderator) {
        if (moderator.getRole() != User.Role.MODERATOR && moderator.getRole() != User.Role.ADMIN) {
            throw new ForbiddenException("Only moderators and admins can resolve reports");
        }

        PostReport report = postReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

        PostReport.Status newStatus;
        try {
            newStatus = PostReport.Status.valueOf(request.getStatus().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid status value: " + request.getStatus());
        }

        if (newStatus == PostReport.Status.PENDING) {
            throw new BadRequestException("Cannot set status back to PENDING");
        }

        report.setStatus(newStatus);
        report.setResolvedBy(moderator.getUsername());
        report.setResolvedAt(LocalDateTime.now());
        return PostReportDto.from(postReportRepository.save(report));
    }
}
