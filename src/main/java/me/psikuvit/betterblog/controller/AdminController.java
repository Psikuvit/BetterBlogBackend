package me.psikuvit.betterblog.controller;

import lombok.RequiredArgsConstructor;
import me.psikuvit.betterblog.entity.ActivityLog;
import me.psikuvit.betterblog.entity.Post;
import me.psikuvit.betterblog.entity.User;
import me.psikuvit.betterblog.exception.BadRequestException;
import me.psikuvit.betterblog.repository.UserRepository;
import me.psikuvit.betterblog.repository.PostRepository;
import me.psikuvit.betterblog.service.ActivityLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080"})
public class AdminController {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final ActivityLogService activityLogService;

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        User currentUser = getCurrentAdmin();

        if (!currentUser.getRole().equals(User.Role.ADMIN)) {
            return ResponseEntity.status(403).build();
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("totalPosts", postRepository.count());
        stats.put("totalPublicPosts", postRepository.countByVisibility(Post.Visibility.PUBLIC));
        stats.put("totalPrivatePosts", postRepository.countByVisibility(Post.Visibility.PRIVATE));
        stats.put("moderatorsCount", userRepository.countByRole(User.Role.MODERATOR));
        stats.put("adminsCount", userRepository.countByRole(User.Role.ADMIN));

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/users")
    public ResponseEntity<Page<User>> getAllUsers(
            @RequestParam(required = false) String role,
            Pageable pageable) {
        User currentUser = getCurrentAdmin();

        if (!currentUser.getRole().equals(User.Role.ADMIN)) {
            return ResponseEntity.status(403).build();
        }

        if (!hasText(role)) {
            return ResponseEntity.ok(userRepository.findAll(pageable));
        }

        User.Role requestedRole;
        try {
            requestedRole = User.Role.valueOf(role.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid role value: " + role);
        }

        List<User> filtered = userRepository.findAll().stream()
                .filter(user -> user.getRole() == requestedRole)
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filtered.size());
        List<User> content = start >= filtered.size() ? List.of() : filtered.subList(start, end);

        return ResponseEntity.ok(new PageImpl<>(content, pageable, filtered.size()));
    }

    @PatchMapping("/users/{id}")
    public ResponseEntity<User> updateUserRole(
            @PathVariable String id,
            @RequestBody Map<String, String> request) {
        User currentUser = getCurrentAdmin();

        if (!currentUser.getRole().equals(User.Role.ADMIN)) {
            return ResponseEntity.status(403).build();
        }

        User user = userRepository.findById(id).orElse(null);
        if (user != null && request != null && hasText(request.get("role"))) {
            try {
                user.setRole(User.Role.valueOf(request.get("role").trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("Invalid role value: " + request.get("role"));
            }
            userRepository.save(user);
        }

        return ResponseEntity.ok(user);
    }

    @GetMapping("/activity")
    public ResponseEntity<Page<ActivityLog>> getSystemActivity(
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String username,
            Pageable pageable) {
        User currentUser = getCurrentAdmin();

        if (!currentUser.getRole().equals(User.Role.ADMIN)) {
            return ResponseEntity.status(403).build();
        }

        if (hasText(username)) {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new BadRequestException("User not found: " + username));
            return ResponseEntity.ok(activityLogService.getUserActivity(user, pageable));
        }

        if (hasText(severity)) {
            ActivityLog.Severity sev;
            try {
                sev = ActivityLog.Severity.valueOf(severity.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("Invalid severity value: " + severity);
            }
            return ResponseEntity.ok(activityLogService.getActivityBySeverity(sev, pageable));
        }

        return ResponseEntity.ok(activityLogService.getAllActivity(pageable));
    }

    private User getCurrentAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new BadRequestException("User is not authenticated");
        }

        return userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new BadRequestException("User not found: " + auth.getName()));
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}

