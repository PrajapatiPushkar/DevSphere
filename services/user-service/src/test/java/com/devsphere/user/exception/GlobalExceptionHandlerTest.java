package com.devsphere.user.exception;

import com.devsphere.user.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest("GET", "/api/v1/resumes/123");
    }

    @Test
    void handleResourceNotFound_Returns404WithStandardErrorResponse() {
        ResourceNotFoundException ex = new ResourceNotFoundException("RESUME_NOT_FOUND", "Resume profile not found");

        ResponseEntity<ErrorResponse> entity = exceptionHandler.handleResourceNotFound(ex, request);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ErrorResponse body = entity.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(404);
        assertThat(body.getError()).isEqualTo("NOT_FOUND");
        assertThat(body.getCode()).isEqualTo("RESUME_NOT_FOUND");
        assertThat(body.getMessage()).isEqualTo("Resume profile not found");
        assertThat(body.getPath()).isEqualTo("/api/v1/resumes/123");
        assertThat(body.getTimestamp()).isNotNull();
    }

    @Test
    void handleUnauthorized_Returns401WithStandardErrorResponse() {
        UnauthorizedException ex = new UnauthorizedException("UNAUTHORIZED", "Authentication required");

        ResponseEntity<ErrorResponse> entity = exceptionHandler.handleUnauthorized(ex, request);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        ErrorResponse body = entity.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(401);
        assertThat(body.getError()).isEqualTo("UNAUTHORIZED");
        assertThat(body.getCode()).isEqualTo("UNAUTHORIZED");
        assertThat(body.getMessage()).isEqualTo("Authentication required");
        assertThat(body.getPath()).isEqualTo("/api/v1/resumes/123");
    }

    @Test
    void handleAccessDenied_Returns403WithStandardErrorResponse() {
        org.springframework.security.access.AccessDeniedException ex =
                new org.springframework.security.access.AccessDeniedException("Forbidden");

        ResponseEntity<ErrorResponse> entity = exceptionHandler.handleAccessDenied(ex, request);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        ErrorResponse body = entity.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(403);
        assertThat(body.getError()).isEqualTo("FORBIDDEN");
        assertThat(body.getCode()).isEqualTo("FORBIDDEN");
        assertThat(body.getMessage()).isEqualTo("You do not have permission to access this resource");
        assertThat(body.getPath()).isEqualTo("/api/v1/resumes/123");
    }

    @Test
    void handleMethodArgumentNotValid_Returns400WithStructuredFieldErrors() {
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("target", "name", "Name cannot be blank");
        when(bindingResult.getFieldErrors()).thenReturn(Collections.singletonList(fieldError));

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<ErrorResponse> entity = exceptionHandler.handleMethodArgumentNotValid(ex, request);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse body = entity.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(400);
        assertThat(body.getError()).isEqualTo("BAD_REQUEST");
        assertThat(body.getCode()).isEqualTo("VALIDATION_ERROR");
        assertThat(body.getMessage()).isEqualTo("name: Name cannot be blank");
        assertThat(body.getErrors()).containsEntry("name", "Name cannot be blank");
        assertThat(body.getPath()).isEqualTo("/api/v1/resumes/123");
    }

    @Test
    void handleResourceNotFound_IncludesTraceIdFromHeaderWhenAvailable() {
        request.addHeader("X-Trace-Id", "test-trace-999");
        ResourceNotFoundException ex = new ResourceNotFoundException("RESUME_NOT_FOUND", "Resume profile not found");

        ResponseEntity<ErrorResponse> entity = exceptionHandler.handleResourceNotFound(ex, request);

        ErrorResponse body = entity.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getTraceId()).isEqualTo("test-trace-999");
    }

    @Test
    void handleGenericException_Returns500WithoutLeakingInternalDetails() {
        RuntimeException ex = new RuntimeException("Sensitive SQL syntax error near 'SELECT * FROM users'");

        ResponseEntity<ErrorResponse> entity = exceptionHandler.handleGenericException(ex, request);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        ErrorResponse body = entity.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(500);
        assertThat(body.getError()).isEqualTo("INTERNAL_SERVER_ERROR");
        assertThat(body.getCode()).isEqualTo("INTERNAL_SERVER_ERROR");
        assertThat(body.getMessage()).isEqualTo("An unexpected internal error occurred");
        assertThat(body.getMessage()).doesNotContain("SQL");
        assertThat(body.getMessage()).doesNotContain("SELECT");
        assertThat(body.getPath()).isEqualTo("/api/v1/resumes/123");
    }
}
