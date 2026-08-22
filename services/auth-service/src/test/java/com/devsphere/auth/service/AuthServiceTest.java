package com.devsphere.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devsphere.auth.dto.LoginRequest;
import com.devsphere.auth.dto.LoginResponse;
import com.devsphere.auth.dto.RegisterRequest;
import com.devsphere.auth.dto.RegisterResponse;
import com.devsphere.auth.entity.UserCredential;
import com.devsphere.auth.event.UserRegisteredDomainEvent;
import com.devsphere.auth.exception.EmailAlreadyExistsException;
import com.devsphere.auth.exception.InvalidCredentialsException;
import com.devsphere.auth.repository.UserCredentialRepository;
import com.devsphere.auth.security.JwtService;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserCredentialRepository userCredentialRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private JwtService jwtService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(
                "devsphere-super-secret-jwt-signing-key-for-local-development-must-be-at-least-256-bits-long",
                3600
        );
        jwtService.init();

        authService = new AuthService(userCredentialRepository, passwordEncoder, jwtService, eventPublisher);
    }

    @Test
    @DisplayName("Should successfully register a new user credential with hashed password and publish domain event")
    void registerNewUserSuccess() {
        RegisterRequest request = new RegisterRequest("testuser@example.com", "SecurePassword123");
        String hashedPassword = "$2a$10$hashedPasswordSample";

        when(userCredentialRepository.existsByEmail("testuser@example.com")).thenReturn(false);
        when(passwordEncoder.encode("SecurePassword123")).thenReturn(hashedPassword);

        UserCredential savedCredential = new UserCredential("testuser@example.com", hashedPassword);
        savedCredential.setId(1L);
        savedCredential.setCreatedAt(Instant.now());

        when(userCredentialRepository.save(any(UserCredential.class))).thenReturn(savedCredential);

        RegisterResponse response = authService.register(request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("testuser@example.com");
        assertThat(response.getCreatedAt()).isNotNull();

        ArgumentCaptor<UserCredential> credentialCaptor = ArgumentCaptor.forClass(UserCredential.class);
        verify(userCredentialRepository).save(credentialCaptor.capture());
        UserCredential captured = credentialCaptor.getValue();

        assertThat(captured.getEmail()).isEqualTo("testuser@example.com");
        assertThat(captured.getPasswordHash()).isEqualTo(hashedPassword);

        verify(eventPublisher).publishEvent(any(UserRegisteredDomainEvent.class));
    }

    @Test
    @DisplayName("Should throw EmailAlreadyExistsException when email is already registered")
    void registerDuplicateEmailThrowsException() {
        RegisterRequest request = new RegisterRequest("existing@example.com", "SecurePassword123");

        when(userCredentialRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessage("An account with this email already exists");
    }

    @Test
    @DisplayName("Should successfully authenticate and return JWT token on valid credentials")
    void loginSuccess() {
        LoginRequest request = new LoginRequest("user@example.com", "SecurePassword123");
        String hashedPassword = "$2a$10$hashedPasswordSample";

        UserCredential credential = new UserCredential("user@example.com", hashedPassword);
        credential.setId(42L);

        when(userCredentialRepository.findByEmail("user@example.com")).thenReturn(Optional.of(credential));
        when(passwordEncoder.matches("SecurePassword123", hashedPassword)).thenReturn(true);

        LoginResponse response = authService.login(request);

        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(3600L);

        var claims = jwtService.parseToken(response.getAccessToken());
        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.get("email", String.class)).isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("Should throw InvalidCredentialsException when email is not found")
    void loginUnknownEmailThrowsException() {
        LoginRequest request = new LoginRequest("unknown@example.com", "SecurePassword123");

        when(userCredentialRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    @DisplayName("Should throw InvalidCredentialsException when password does not match")
    void loginWrongPasswordThrowsException() {
        LoginRequest request = new LoginRequest("user@example.com", "WrongPassword");
        String hashedPassword = "$2a$10$hashedPasswordSample";

        UserCredential credential = new UserCredential("user@example.com", hashedPassword);

        when(userCredentialRepository.findByEmail("user@example.com")).thenReturn(Optional.of(credential));
        when(passwordEncoder.matches("WrongPassword", hashedPassword)).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
    }
}
