package me.psikuvit.betterblog.controller;

import lombok.RequiredArgsConstructor;
import me.psikuvit.betterblog.entity.ActivityLog;
import me.psikuvit.betterblog.entity.User;
import me.psikuvit.betterblog.exception.UnauthorizedException;
import me.psikuvit.betterblog.service.ActivityLogService;
import me.psikuvit.betterblog.service.AuthService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/activity")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080"})
public class ActivityLogController {

	private final ActivityLogService activityLogService;
	private final AuthService authService;

	@GetMapping
	public ResponseEntity<Page<ActivityLog>> getMyActivity(
			@RequestParam(required = false) String action,
			Pageable pageable) {
		User currentUser = getCurrentUser();

		if (action != null && !action.trim().isEmpty()) {
			return ResponseEntity.ok(activityLogService.getUserActivityByAction(currentUser, action, pageable));
		}

		return ResponseEntity.ok(activityLogService.getUserActivity(currentUser, pageable));
	}

	private User getCurrentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new UnauthorizedException("User is not authenticated");
		}

		return authService.getUserEntityByUsername(authentication.getName());
	}
}


