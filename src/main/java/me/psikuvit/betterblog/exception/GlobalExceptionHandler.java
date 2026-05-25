package me.psikuvit.betterblog.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import me.psikuvit.betterblog.exception.ForbiddenException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {
        log.info("Resource not found: {} - {}", ex.getMessage(), request.getDescription(false));
        ErrorResponse errorResponse = ErrorResponse.builder()
                .code("USER_NOT_FOUND")
                .status(HttpStatus.NOT_FOUND.value())
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequestException(
            BadRequestException ex, WebRequest request) {
        log.warn("Bad request: {} - {}", ex.getMessage(), request.getDescription(false));
        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(resolveBadRequestCode(ex.getMessage()))
                .status(HttpStatus.BAD_REQUEST.value())
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedException(
            UnauthorizedException ex, WebRequest request) {
        log.warn("Unauthorized: {} - {}", ex.getMessage(), request.getDescription(false));
        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(resolveUnauthorizedCode(ex.getMessage()))
                .status(HttpStatus.UNAUTHORIZED.value())
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleAlreadyExistsException(
            AlreadyExistsException ex, WebRequest request) {
        log.warn("Conflict / already exists: {} - {}", ex.getMessage(), request.getDescription(false));
        ErrorResponse errorResponse = ErrorResponse.builder()
                .code("ALREADY_EXISTS")
                .status(HttpStatus.CONFLICT.value())
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitExceededException(
            RateLimitExceededException ex, WebRequest request) {
        log.warn("Rate limit exceeded: {} - {}", ex.getMessage(), request.getDescription(false));
        ErrorResponse errorResponse = ErrorResponse.builder()
                .code("RATE_LIMIT_EXCEEDED")
                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.TOO_MANY_REQUESTS);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, WebRequest request) {
        log.warn("Validation failed: {} - {}", ex.getMessage(), request.getDescription(false));
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code("VALIDATION_FAILED")
                .status(HttpStatus.BAD_REQUEST.value())
                .message("Validation failed")
                .errors(errors)
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, WebRequest request) {
        log.warn("Method not allowed: {} - {}", ex.getMethod(), request.getDescription(false));
        ErrorResponse errorResponse = ErrorResponse.builder()
                .code("METHOD_NOT_ALLOWED")
                .status(HttpStatus.METHOD_NOT_ALLOWED.value())
                .message("Request method '" + ex.getMethod() + "' is not supported for this endpoint")
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbiddenException(
            ForbiddenException ex, WebRequest request) {
        log.warn("Forbidden: {} - {}", ex.getMessage(), request.getDescription(false));
        ErrorResponse errorResponse = ErrorResponse.builder()
                .code("FORBIDDEN")
                .status(HttpStatus.FORBIDDEN.value())
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex, WebRequest request) {
        log.error("Unhandled exception at {}: {}", request.getDescription(false), ex.getMessage(), ex);
        ErrorResponse errorResponse = ErrorResponse.builder()
                .code("INTERNAL_SERVER_ERROR")
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .message("An unexpected error occurred")
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private String resolveBadRequestCode(String message) {
        if (message == null) {
            return "BAD_REQUEST";
        }

        String normalized = message.toLowerCase();
        if (normalized.contains("either username or email is required")) {
            return "LOGIN_IDENTIFIER_REQUIRED";
        }

        if (normalized.contains("reset code has expired")) {
            return "PASSWORD_RESET_CODE_EXPIRED";
        }

        if (normalized.contains("invalid reset code")) {
            return "PASSWORD_RESET_CODE_INVALID";
        }

        if (normalized.contains("reset code is missing") || normalized.contains("reset code is missing or expired")) {
            return "PASSWORD_RESET_CODE_MISSING";
        }

        return "BAD_REQUEST";
    }

    private String resolveUnauthorizedCode(String message) {
        if (message == null) {
            return "UNAUTHORIZED";
        }

        String normalized = message.toLowerCase();
        if (normalized.contains("disabled")) {
            return "USER_DISABLED";
        }
        if (normalized.contains("invalid password")) {
            return "INVALID_PASSWORD";
        }
        if (normalized.contains("token")) {
            return "INVALID_TOKEN";
        }

        return "UNAUTHORIZED";
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ErrorResponse {
        private String code;
        private int status;
        private String message;
        private LocalDateTime timestamp;
        private String path;
        private Map<String, String> errors;
    }
}

