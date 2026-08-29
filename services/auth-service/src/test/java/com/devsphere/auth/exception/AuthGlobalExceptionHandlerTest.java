package com.devsphere.auth.exception;

import com.devsphere.auth.dto.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class AuthGlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
    }

    @Test
    void handleEmailAlreadyExists_Returns409WithStandardErrorResponse() {
        EmailAlreadyExistsException ex = new EmailAlreadyExistsException("An account with this email already exists");

        ResponseEntity<ErrorResponse> entity = exceptionHandler.handleEmailAlreadyExists(ex, request);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        ErrorResponse body = entity.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(409);
        assertThat(body.getError()).isEqualTo("CONFLICT");
        assertThat(body.getCode()).isEqualTo("EMAIL_ALREADY_EXISTS");
        assertThat(body.getMessage()).isEqualTo("An account with this email already exists");
        assertThat(body.getPath()).isEqualTo("/api/v1/auth/login");
    }

    @Test
    void handleInvalidCredentials_Returns401WithStandardErrorResponse() {
        InvalidCredentialsException ex = new InvalidCredentialsException("Invalid email or password");

        ResponseEntity<ErrorResponse> entity = exceptionHandler.handleInvalidCredentials(ex, request);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        ErrorResponse body = entity.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(401);
        assertThat(body.getError()).isEqualTo("UNAUTHORIZED");
        assertThat(body.getCode()).isEqualTo("INVALID_CREDENTIALS");
        assertThat(body.getMessage()).isEqualTo("Invalid email or password");
        assertThat(body.getPath()).isEqualTo("/api/v1/auth/login");
    }

    @Test
    void handleGenericException_Returns500WithGenericMessage() {
        RuntimeException ex = new RuntimeException("Database connection timeout");

        ResponseEntity<ErrorResponse> entity = exceptionHandler.handleGenericException(ex, request);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        ErrorResponse body = entity.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(500);
        assertThat(body.getError()).isEqualTo("INTERNAL_SERVER_ERROR");
        assertThat(body.getCode()).isEqualTo("INTERNAL_SERVER_ERROR");
        assertThat(body.getMessage()).isEqualTo("An unexpected error occurred. Please try again later.");
        assertThat(body.getMessage()).doesNotContain("Database");
        assertThat(body.getPath()).isEqualTo("/api/v1/auth/login");
    }
}
