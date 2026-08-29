package com.devsphere.user.exception;

import com.devsphere.user.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found for path {}: {}", getPath(request), ex.getMessage());
        ErrorResponse response = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.name(),
                ex.getCode() != null ? ex.getCode() : "RESOURCE_NOT_FOUND",
                ex.getMessage(),
                getPath(request),
                null,
                getTraceId()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException ex, HttpServletRequest request) {
        log.warn("Unauthorized request for path {}: {}", getPath(request), ex.getMessage());
        ErrorResponse response = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.name(),
                ex.getCode() != null ? ex.getCode() : "UNAUTHORIZED",
                ex.getMessage(),
                getPath(request),
                null,
                getTraceId()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied for path {}: {}", getPath(request), ex.getMessage());
        ErrorResponse response = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.name(),
                "FORBIDDEN",
                "You do not have permission to access this resource",
                getPath(request),
                null,
                getTraceId()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        String primaryMessage = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Validation error occurred");

        log.warn("Validation failed for path {}: {}", getPath(request), primaryMessage);
        ErrorResponse response = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.name(),
                "VALIDATION_FAILED",
                primaryMessage,
                getPath(request),
                fieldErrors.isEmpty() ? null : fieldErrors,
                getTraceId()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            fieldErrors.put(violation.getPropertyPath().toString(), violation.getMessage());
        }

        log.warn("Constraint violation for path {}: {}", getPath(request), ex.getMessage());
        ErrorResponse response = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.name(),
                "VALIDATION_FAILED",
                "Request validation failed",
                getPath(request),
                fieldErrors.isEmpty() ? null : fieldErrors,
                getTraceId()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Invalid argument for path {}: {}", getPath(request), ex.getMessage());
        ErrorResponse response = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.name(),
                "BAD_REQUEST",
                ex.getMessage(),
                getPath(request),
                null,
                getTraceId()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex, HttpServletRequest request) {
        log.warn("Illegal state operation for path {}: {}", getPath(request), ex.getMessage());
        ErrorResponse response = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.name(),
                "BAD_REQUEST",
                ex.getMessage(),
                getPath(request),
                null,
                getTraceId()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(DuplicatePlannerEntryException.class)
    public ResponseEntity<ErrorResponse> handleDuplicatePlannerEntry(DuplicatePlannerEntryException ex, HttpServletRequest request) {
        log.warn("Duplicate planner entry for path {}: {}", getPath(request), ex.getMessage());
        ErrorResponse response = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.name(),
                ex.getCode() != null ? ex.getCode() : "DUPLICATE_PLANNER_ENTRY",
                ex.getMessage(),
                getPath(request),
                null,
                getTraceId()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(DuplicateDsaProblemException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateDsaProblem(DuplicateDsaProblemException ex, HttpServletRequest request) {
        log.warn("Duplicate DSA problem for path {}: {}", getPath(request), ex.getMessage());
        ErrorResponse response = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.name(),
                ex.getCode() != null ? ex.getCode() : "DUPLICATE_DSA_PROBLEM",
                ex.getMessage(),
                getPath(request),
                null,
                getTraceId()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(DuplicateSkillException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateSkill(DuplicateSkillException ex, HttpServletRequest request) {
        log.warn("Duplicate skill for path {}: {}", getPath(request), ex.getMessage());
        ErrorResponse response = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.name(),
                ex.getCode() != null ? ex.getCode() : "DUPLICATE_SKILL",
                ex.getMessage(),
                getPath(request),
                null,
                getTraceId()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(DuplicateResumeSelectionException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResumeSelection(DuplicateResumeSelectionException ex, HttpServletRequest request) {
        log.warn("Duplicate resume selection for path {}: {}", getPath(request), ex.getMessage());
        ErrorResponse response = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.name(),
                ex.getCode() != null ? ex.getCode() : "DUPLICATE_RESUME_SELECTION",
                ex.getMessage(),
                getPath(request),
                null,
                getTraceId()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler({OptimisticLockingFailureException.class, ObjectOptimisticLockingFailureException.class})
    public ResponseEntity<ErrorResponse> handleOptimisticLockingFailure(Exception ex, HttpServletRequest request) {
        log.warn("Optimistic locking failure for path {}: {}", getPath(request), ex.getMessage());
        ErrorResponse response = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.name(),
                "RESOURCE_VERSION_CONFLICT",
                "The resource was modified by another request",
                getPath(request),
                null,
                getTraceId()
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
                getTraceId()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler({PessimisticLockingFailureException.class, CannotAcquireLockException.class})
    public ResponseEntity<ErrorResponse> handlePessimisticLockingFailure(Exception ex, HttpServletRequest request) {
        log.warn("Pessimistic lock acquisition failure for path {}: {}", getPath(request), ex.getMessage());
        ErrorResponse response = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.name(),
                "LOCK_ACQUISITION_TIMEOUT",
                "Could not acquire database lock within timeout",
                getPath(request),
                null,
                getTraceId()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("Malformed JSON payload for path {}: {}", getPath(request), ex.getMessage());
        ErrorResponse response = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.name(),
                "MALFORMED_REQUEST",
                "Malformed JSON request payload or invalid data format",
                getPath(request),
                null,
                getTraceId()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String typeName = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "valid type";
        String message = String.format("Parameter '%s' should be of type %s", ex.getName(), typeName);
        log.warn("Parameter type mismatch for path {}: {}", getPath(request), message);
        ErrorResponse response = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.name(),
                "INVALID_PARAMETER",
                message,
                getPath(request),
                null,
                getTraceId()
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
                getTraceId()
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
                getTraceId()
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
                getTraceId()
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @ExceptionHandler({io.github.resilience4j.timelimiter.RequestTimeoutException.class, java.util.concurrent.TimeoutException.class})
    public ResponseEntity<ErrorResponse> handleTimeout(Exception ex, HttpServletRequest request) {
        log.warn("Request timed out for path {}: {}", getPath(request), ex.getMessage());
        ErrorResponse response = new ErrorResponse(
                HttpStatus.GATEWAY_TIMEOUT.value(),
                HttpStatus.GATEWAY_TIMEOUT.name(),
                "DOWNSTREAM_TIMEOUT",
                "Operation timed out while processing request",
                getPath(request),
                null,
                getTraceId()
        );
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception in User Service for path {}: {}", getPath(request), ex.getMessage(), ex);
        ErrorResponse response = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.name(),
                "INTERNAL_SERVER_ERROR",
                "An unexpected internal error occurred",
                getPath(request),
                null,
                getTraceId()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private String getPath(HttpServletRequest request) {
        return request != null ? request.getRequestURI() : null;
    }

    private String getTraceId() {
        try {
            String traceId = MDC.get("traceId");
            if (traceId != null && !traceId.isBlank()) {
                return traceId;
            }
            return MDC.get("trace_id");
        } catch (Exception e) {
            return null;
        }
    }
}
