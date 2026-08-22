package com.devsphere.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devsphere.auth.dto.RegisterRequest;
import com.devsphere.auth.dto.RegisterResponse;
import com.devsphere.auth.entity.UserCredential;
import com.devsphere.auth.exception.EmailAlreadyExistsException;
import com.devsphere.auth.repository.UserCredentialRepository;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserCredentialRepository userCredentialRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userCredentialRepository, passwordEncoder);
    }

    @Test
    @DisplayName("Should successfully register a new user credential with hashed password")
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
}
