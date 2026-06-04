package me.psikuvit.betterblog.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.psikuvit.betterblog.dto.PostReportDto;
import me.psikuvit.betterblog.dto.ResolveReportRequest;
import me.psikuvit.betterblog.entity.User;
import me.psikuvit.betterblog.exception.ForbiddenException;
import me.psikuvit.betterblog.exception.UnauthorizedException;
import me.psikuvit.betterblog.repository.UserRepository;
import me.psikuvit.betterblog.service.PostReportService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/moderator")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080"})
public class ModeratorController {

    private final PostReportService postReportService;
    private final UserRepository userRepository;

    @GetMapping("/reports")
    public ResponseEntity<Page<PostReportDto>> getReportedPosts(
            @RequestParam(required = false) String status,
            Pageable pageable) {
        requireModerator();
        return ResponseEntity.ok(postReportService.getReports(status, pageable));
    }

    @PatchMapping("/reports/{id}")
    public ResponseEntity<PostReportDto> resolveReport(
            @PathVariable String id,
            @Valid @RequestBody ResolveReportRequest request) {
        User moderator = requireModerator();
        return ResponseEntity.ok(postReportService.resolveReport(id, request, moderator));
    }

    private User requireModerator() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new UnauthorizedException("User is not authenticated");
        }

        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        if (user.getRole() != User.Role.MODERATOR && user.getRole() != User.Role.ADMIN) {
            throw new ForbiddenException("Moderator access required");
        }

        return user;
    }
}
