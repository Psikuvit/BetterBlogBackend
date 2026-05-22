package me.psikuvit.betterblog.controller;

import lombok.RequiredArgsConstructor;
import me.psikuvit.betterblog.dto.ApiTokenRequest;
import me.psikuvit.betterblog.entity.ApiToken;
import me.psikuvit.betterblog.entity.User;
import me.psikuvit.betterblog.exception.UnauthorizedException;
import me.psikuvit.betterblog.service.ApiTokenService;
import me.psikuvit.betterblog.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tokens")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080"})
public class ApiTokenController {

	private final ApiTokenService apiTokenService;
	private final AuthService authService;

	@GetMapping
	public ResponseEntity<List<ApiToken>> getMyTokens() {
		return ResponseEntity.ok(apiTokenService.getUserTokens(getCurrentUser()));
	}

	@PostMapping
	public ResponseEntity<ApiToken> createToken(@RequestBody ApiTokenRequest request) {
		ApiToken token = apiTokenService.createApiToken(request, getCurrentUser());
		return ResponseEntity.status(HttpStatus.CREATED).body(token);
	}

	@DeleteMapping("/{tokenId}")
	public ResponseEntity<Map<String, String>> revokeToken(@PathVariable String tokenId) {
		apiTokenService.revokeApiToken(tokenId, getCurrentUser());

		Map<String, String> response = new HashMap<>();
		response.put("message", "Token revoked");
		return ResponseEntity.ok(response);
	}

	private User getCurrentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new UnauthorizedException("User is not authenticated");
		}

		return authService.getUserEntityByUsername(authentication.getName());
	}
}

