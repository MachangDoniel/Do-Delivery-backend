package com.dodelivery.app.exception;

import com.dodelivery.app.dto.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Centralised exception handler.
 *
 * <p>All exceptions are mapped to a consistent {@link ApiResponse} error envelope.
 * Stack traces are never leaked to clients.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── Domain exceptions ────────────────────────────────────────────────────

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(
            ResourceNotFoundException ex, HttpServletRequest req) {
        return buildError(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), req);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(
            BusinessException ex, HttpServletRequest req) {
        return buildError(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), req);
    }

    @ExceptionHandler(OtpException.class)
    public ResponseEntity<ApiResponse<Void>> handleOtp(
            OtpException ex, HttpServletRequest req) {
        return buildError(HttpStatus.UNAUTHORIZED, "Unauthorized", ex.getMessage(), req);
    }

    // ── Spring Security exceptions ────────────────────────────────────────────

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest req) {
        return buildError(HttpStatus.FORBIDDEN, "Forbidden",
                "You do not have permission to perform this action", req);
    }

        @ExceptionHandler(NoResourceFoundException.class)
        public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(
            NoResourceFoundException ex, HttpServletRequest req) {
        return buildError(HttpStatus.NOT_FOUND, "Not Found",
            "Endpoint not found", req);
        }

    // ── Validation exceptions ─────────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest req) {

        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .sorted()
                .collect(Collectors.joining("; "));

        return buildError(HttpStatus.BAD_REQUEST, "Validation Failed", message, req);
    }

    // ── Infrastructure exceptions ───────────────────────────────────────────

    @ExceptionHandler({
            RedisConnectionFailureException.class,
            RedisSystemException.class,
            DataAccessResourceFailureException.class,
            CannotGetJdbcConnectionException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleInfrastructure(
            Exception ex, HttpServletRequest req) {
        log.error("Infrastructure dependency failure on {} {}", req.getMethod(), req.getRequestURI(), ex);
        return buildError(HttpStatus.SERVICE_UNAVAILABLE,
                "Service Unavailable",
                "Temporary service unavailable. Please try again.",
                req);
    }

    // ── Catch-all ─────────────────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(
            Exception ex, HttpServletRequest req) {
        // Log full stack trace server-side but never expose it to the client
        log.error("Unhandled exception on {} {}", req.getMethod(), req.getRequestURI(), ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "An unexpected error occurred", req);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private ResponseEntity<ApiResponse<Void>> buildError(
            @NonNull HttpStatusCode status, String error, String message, HttpServletRequest req) {
        return ResponseEntity
                .status(status)
                .body(ApiResponse.error(status.value(), error, message, req.getRequestURI()));
    }
}
