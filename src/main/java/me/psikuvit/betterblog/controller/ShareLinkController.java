package me.psikuvit.betterblog.controller;

import lombok.RequiredArgsConstructor;
import me.psikuvit.betterblog.dto.ShareLinkRequest;
import me.psikuvit.betterblog.entity.Post;
import me.psikuvit.betterblog.entity.ShareLink;
import me.psikuvit.betterblog.entity.User;
import me.psikuvit.betterblog.exception.UnauthorizedException;
import me.psikuvit.betterblog.service.AuthService;
import me.psikuvit.betterblog.service.ShareLinkService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/sharing")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080"})
public class ShareLinkController {

	private final ShareLinkService shareLinkService;
	private final AuthService authService;

	@GetMapping("/links")
	public ResponseEntity<Page<ShareLink>> getMyLinks(Pageable pageable) {
		return ResponseEntity.ok(shareLinkService.getUserShareLinks(getCurrentUser(), pageable));
	}

	@PostMapping("/links")
	public ResponseEntity<ShareLink> createLink(@RequestBody ShareLinkRequest request) {
		ShareLink created = shareLinkService.createShareLink(request, getCurrentUser());
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	@DeleteMapping("/links/{linkId}")
	public ResponseEntity<Map<String, String>> revokeLink(@PathVariable String linkId) {
		shareLinkService.revokeShareLink(linkId, getCurrentUser());

		Map<String, String> response = new HashMap<>();
		response.put("message", "Link revoked");
		return ResponseEntity.ok(response);
	}

	@GetMapping("/access/{token}")
	public ResponseEntity<Post> accessLink(@PathVariable String token) {
		return ResponseEntity.ok(shareLinkService.accessViaLink(token));
	}

	private User getCurrentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new UnauthorizedException("User is not authenticated");
		}

		return authService.getUserEntityByUsername(authentication.getName());
	}
}

