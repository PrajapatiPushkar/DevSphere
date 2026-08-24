package com.devsphere.auth.tracing;

import com.devsphere.auth.dto.LoginRequest;
import com.devsphere.auth.dto.LoginResponse;
import com.devsphere.auth.dto.RegisterRequest;
import com.devsphere.auth.dto.RegisterResponse;
import com.devsphere.auth.entity.UserCredential;
import com.devsphere.auth.outbox.OutboxService;
import com.devsphere.auth.repository.UserCredentialRepository;
import com.devsphere.auth.security.JwtService;
import com.devsphere.auth.service.AuthService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.tracing.ScopedSpan;
import io.micrometer.tracing.Tracer;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTracingTest {

    private UserCredentialRepository userCredentialRepository;
    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private OutboxService outboxService;
    private SimpleMeterRegistry meterRegistry;
    private Tracer tracer;
    private ScopedSpan scopedSpan;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userCredentialRepository = Mockito.mock(UserCredentialRepository.class);
        passwordEncoder = Mockito.mock(PasswordEncoder.class);
        jwtService = Mockito.mock(JwtService.class);
        outboxService = Mockito.mock(OutboxService.class);
        meterRegistry = new SimpleMeterRegistry();
        tracer = Mockito.mock(Tracer.class);
        scopedSpan = Mockito.mock(ScopedSpan.class);

        when(tracer.startScopedSpan(anyString())).thenReturn(scopedSpan);

        authService = new AuthService(
                userCredentialRepository,
                passwordEncoder,
                jwtService,
                outboxService,
                meterRegistry,
                tracer
        );
    }

    @Test
    @DisplayName("Should create auth.registration span on user registration and attach safe tags")
    void testRegistrationCreatesSpan() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("tracer@example.com");
        request.setPassword("SecretPass123!");

        when(userCredentialRepository.existsByEmail("tracer@example.com")).thenReturn(false);
        when(passwordEncoder.encode("SecretPass123!")).thenReturn("hashed-pass");

        UserCredential savedCredential = new UserCredential("tracer@example.com", "hashed-pass", "USER");
        savedCredential.setId(100L);
        when(userCredentialRepository.save(any(UserCredential.class))).thenReturn(savedCredential);

        RegisterResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(100L);

        verify(tracer).startScopedSpan("auth.registration");
        verify(scopedSpan).tag("service.operation", "register");
        verify(scopedSpan).tag("event.type", "UserRegisteredEvent");
        verify(scopedSpan).end();

        // Sensitive data protection verification: password, hash must never be passed to tag
        verify(scopedSpan, never()).tag(anyString(), Mockito.eq("SecretPass123!"));
        verify(scopedSpan, never()).tag(anyString(), Mockito.eq("hashed-pass"));
    }

    @Test
    @DisplayName("Should create auth.login span on user login and attach safe tags")
    void testLoginCreatesSpan() {
        LoginRequest request = new LoginRequest();
        request.setEmail("tracer@example.com");
        request.setPassword("SecretPass123!");

        UserCredential credential = new UserCredential("tracer@example.com", "hashed-pass", "USER");
        credential.setId(100L);

        when(userCredentialRepository.findByEmail("tracer@example.com")).thenReturn(Optional.of(credential));
        when(passwordEncoder.matches("SecretPass123!", "hashed-pass")).thenReturn(true);
        when(jwtService.generateToken(100L, "tracer@example.com", "USER")).thenReturn("jwt-token-123");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        LoginResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("jwt-token-123");

        verify(tracer).startScopedSpan("auth.login");
        verify(scopedSpan).tag("service.operation", "login");
        verify(scopedSpan).end();

        // Sensitive data protection verification: JWT token, password must never be tagged
        verify(scopedSpan, never()).tag(anyString(), Mockito.eq("jwt-token-123"));
        verify(scopedSpan, never()).tag(anyString(), Mockito.eq("SecretPass123!"));
    }
}

