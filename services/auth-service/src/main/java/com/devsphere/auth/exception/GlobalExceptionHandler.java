package com.devsphere.auth.exception;

import com.devsphere.auth.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyExists(EmailAlreadyExistsException ex, HttpServletRequest request) {
        log.warn("Email already exists for path {}: {}", getPath(request), ex.getMessage());
        ErrorResponse response = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.name(),
                "EMAIL_ALREADY_EXISTS",
                ex.getMessage(),
                getPath(request),
                null,
                getTraceId(request)
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Database constraint violation for path {}: {}", getPath(request), ex.getMessage());
        ErrorResponse response = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.name(),
                "DATABASE_CONSTRAINT_VIOLATION",
                "Database constraint violation occurred",
                getPath(request),
                null,
                getTraceId(request)
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex, HttpServletRequest request) {
        log.warn("Invalid credentials for path {}: {}", getPath(request), ex.getMessage());
        ErrorResponse response = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.name(),
                "INVALID_CREDENTIALS",
                ex.getMessage(),
                getPath(request),
                null,
                getTraceId(request)
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        log.warn("Validation error for path {}: {}", getPath(request), errors);
        ErrorResponse response = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.name(),
                "VALIDATION_ERROR",
                "Request validation failed",
                getPath(request),
                errors,
                getTraceId(request)
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("Malformed request payload for path {}: {}", getPath(request), ex.getMessage());
        ErrorResponse response = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.name(),
                "MALFORMED_REQUEST",
                "Malformed JSON request payload or invalid data format",
                getPath(request),
                null,
                getTraceId(request)
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        log.warn("HTTP method not supported for path {}: {}", getPath(request), ex.getMessage());
        ErrorResponse response = new ErrorResponse(
                HttpStatus.METHOD_NOT_ALLOWED.value(),
                HttpStatus.METHOD_NOT_ALLOWED.name(),
                "METHOD_NOT_ALLOWED",
                "HTTP method " + ex.getMethod() + " is not supported for this endpoint",
                getPath(request),
                null,
                getTraceId(request)
        );
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }

    @ExceptionHandler(io.github.resilience4j.bulkhead.BulkheadFullException.class)
    public ResponseEntity<ErrorResponse> handleBulkheadFull(io.github.resilience4j.bulkhead.BulkheadFullException ex, HttpServletRequest request) {
        log.warn("Bulkhead limit reached for path {}: {}", getPath(request), ex.getMessage());
        ErrorResponse response = new ErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                HttpStatus.SERVICE_UNAVAILABLE.name(),
                "BULKHEAD_LIMIT_EXCEEDED",
                "System rate or concurrency limit exceeded. Please try again later.",
                getPath(request),
                null,
                getTraceId(request)
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @ExceptionHandler(io.github.resilience4j.circuitbreaker.CallNotPermittedException.class)
    public ResponseEntity<ErrorResponse> handleCallNotPermitted(io.github.resilience4j.circuitbreaker.CallNotPermittedException ex, HttpServletRequest request) {
        log.warn("Circuit breaker OPEN for path {}: {}", getPath(request), ex.getMessage());
        ErrorResponse response = new ErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                HttpStatus.SERVICE_UNAVAILABLE.name(),
                "DOWNSTREAM_SERVICE_UNAVAILABLE",
                "Downstream service circuit breaker is OPEN",
                getPath(request),
                null,
                getTraceId(request)
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @ExceptionHandler(io.github.resilience4j.ratelimiter.RequestNotPermitted.class)
    public ResponseEntity<ErrorResponse> handleRequestNotPermitted(io.github.resilience4j.ratelimiter.RequestNotPermitted ex, HttpServletRequest request) {
        log.warn("Rate limit exceeded for path {}: {}", getPath(request), ex.getMessage());
        ErrorResponse response = new ErrorResponse(
                HttpStatus.TOO_MANY_REQUESTS.value(),
                HttpStatus.TOO_MANY_REQUESTS.name(),
                "RATE_LIMIT_EXCEEDED",
                "Rate limit exceeded. Please try again later.",
                getPath(request),
                null,
                getTraceId(request)
        );
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
    }

    @ExceptionHandler(java.util.concurrent.TimeoutException.class)
    public ResponseEntity<ErrorResponse> handleTimeout(Exception ex, HttpServletRequest request) {
        log.warn("Request timed out for path {}: {}", getPath(request), ex.getMessage());
        ErrorResponse response = new ErrorResponse(
                HttpStatus.GATEWAY_TIMEOUT.value(),
                HttpStatus.GATEWAY_TIMEOUT.name(),
                "DOWNSTREAM_TIMEOUT",
                "Operation timed out while processing request",
                getPath(request),
                null,
                getTraceId(request)
        );
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception in Auth Service for path {}: {}", getPath(request), ex.getMessage(), ex);
        ErrorResponse response = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.name(),
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred. Please try again later.",
                getPath(request),
                null,
                getTraceId(request)
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private String getPath(HttpServletRequest request) {
        return request != null ? request.getRequestURI() : null;
    }

    private String getTraceId() {
        return getTraceId(null);
    }

    private String getTraceId(HttpServletRequest request) {
        try {
            String traceId = MDC.get("traceId");
            if (traceId != null && !traceId.isBlank()) {
                return traceId;
            }
            traceId = MDC.get("trace_id");
            if (traceId != null && !traceId.isBlank()) {
                return traceId;
            }
            if (request != null) {
                traceId = request.getHeader("X-Trace-Id");
                if (traceId != null && !traceId.isBlank()) {
                    return traceId;
                }
                String traceparent = request.getHeader("traceparent");
                if (traceparent != null && !traceparent.isBlank()) {
                    String[] parts = traceparent.split("-");
                    if (parts.length >= 2 && !parts[1].isBlank()) {
                        return parts[1];
                    }
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
