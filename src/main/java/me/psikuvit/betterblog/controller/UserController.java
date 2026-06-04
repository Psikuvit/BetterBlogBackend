package me.psikuvit.betterblog.controller;

import lombok.RequiredArgsConstructor;
import me.psikuvit.betterblog.dto.UserDto;
import me.psikuvit.betterblog.entity.Post;
import me.psikuvit.betterblog.entity.User;
import me.psikuvit.betterblog.service.AuthService;
import me.psikuvit.betterblog.service.PostService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080"})
public class UserController {

    private final AuthService authService;
    private final PostService postService;

    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserDto user = authService.getUserByUsername(auth.getName());
        return ResponseEntity.ok(user);
    }

    @GetMapping("/{username}")
    public ResponseEntity<UserDto> getUserProfile(@PathVariable String username) {
        UserDto user = authService.getUserByUsername(username);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/{username}/posts")
    public ResponseEntity<Page<Post>> getUserPosts(
            @PathVariable String username,
            Pageable pageable) {
        User profileUser = authService.getUserEntityByUsername(username);
        return ResponseEntity.ok(postService.getUserPosts(profileUser, getOptionalUser(), pageable));
    }

    private User getOptionalUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null
                || !auth.isAuthenticated()
                || !(auth.getPrincipal() instanceof org.springframework.security.core.userdetails.User)) {
            return null;
        }

        try {
            return authService.getUserEntityByUsername(auth.getName());
        } catch (Exception ex) {
            return null;
        }
    }
}

