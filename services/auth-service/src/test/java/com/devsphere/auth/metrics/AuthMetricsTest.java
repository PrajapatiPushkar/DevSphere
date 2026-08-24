package com.devsphere.auth.metrics;

import com.devsphere.auth.dto.LoginRequest;
import com.devsphere.auth.dto.LoginResponse;
import com.devsphere.auth.dto.RegisterRequest;
import com.devsphere.auth.dto.RegisterResponse;
import com.devsphere.auth.entity.UserCredential;
import com.devsphere.auth.outbox.OutboxService;
import com.devsphere.auth.repository.UserCredentialRepository;
import com.devsphere.auth.security.JwtService;
import com.devsphere.auth.service.AuthService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class AuthMetricsTest {

    private UserCredentialRepository userCredentialRepository;
    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private OutboxService outboxService;
    private MeterRegistry meterRegistry;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userCredentialRepository = Mockito.mock(UserCredentialRepository.class);
        passwordEncoder = Mockito.mock(PasswordEncoder.class);
        jwtService = Mockito.mock(JwtService.class);
        outboxService = Mockito.mock(OutboxService.class);
        meterRegistry = new SimpleMeterRegistry();

        authService = new AuthService(userCredentialRepository, passwordEncoder, jwtService, outboxService, meterRegistry);
    }

    @Test
    void register_IncrementsSuccessMetric() {
        RegisterRequest request = new RegisterRequest("user@example.com", "Password123");
        when(userCredentialRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn("hashedPassword");

        UserCredential savedCredential = new UserCredential("user@example.com", "hashedPassword");
        savedCredential.setId(100L);
        when(userCredentialRepository.save(any(UserCredential.class))).thenReturn(savedCredential);

        RegisterResponse response = authService.register(request);

        assertThat(response).isNotNull();
        double count = meterRegistry.counter("devsphere.auth.registration.total", "status", "success").count();
        assertThat(count).isEqualTo(1.0);
    }

    @Test
    void login_IncrementsSuccessMetric() {
        LoginRequest request = new LoginRequest("user@example.com", "Password123");
        UserCredential credential = new UserCredential("user@example.com", "hashedPassword");
        credential.setId(100L);

        when(userCredentialRepository.findByEmail("user@example.com")).thenReturn(Optional.of(credential));
        when(passwordEncoder.matches("Password123", "hashedPassword")).thenReturn(true);
        when(jwtService.generateToken(100L, "user@example.com")).thenReturn("jwt-token");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        LoginResponse response = authService.login(request);

        assertThat(response).isNotNull();
        double count = meterRegistry.counter("devsphere.auth.login.total", "status", "success").count();
        assertThat(count).isEqualTo(1.0);
    }
}
