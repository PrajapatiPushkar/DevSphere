package com.devsphere.auth.service;

import com.devsphere.auth.dto.LoginRequest;
import com.devsphere.auth.dto.LoginResponse;
import com.devsphere.auth.dto.RegisterRequest;
import com.devsphere.auth.dto.RegisterResponse;
import com.devsphere.auth.entity.UserCredential;
import com.devsphere.auth.exception.EmailAlreadyExistsException;
import com.devsphere.auth.exception.InvalidCredentialsException;
import com.devsphere.auth.outbox.OutboxService;
import com.devsphere.auth.repository.UserCredentialRepository;
import com.devsphere.auth.security.JwtService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.tracing.ScopedSpan;
import io.micrometer.tracing.Tracer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserCredentialRepository userCredentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OutboxService outboxService;
    private final MeterRegistry meterRegistry;
    private final Tracer tracer;

    public AuthService(UserCredentialRepository userCredentialRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       OutboxService outboxService) {
        this(userCredentialRepository, passwordEncoder, jwtService, outboxService, new SimpleMeterRegistry(), null);
    }

    public AuthService(UserCredentialRepository userCredentialRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       OutboxService outboxService,
                       MeterRegistry meterRegistry) {
        this(userCredentialRepository, passwordEncoder, jwtService, outboxService, meterRegistry, null);
    }

    @Autowired(required = false)
    public AuthService(UserCredentialRepository userCredentialRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       OutboxService outboxService,
                       MeterRegistry meterRegistry,
                       Tracer tracer) {
        this.userCredentialRepository = userCredentialRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.outboxService = outboxService;
        this.meterRegistry = meterRegistry;
        this.tracer = tracer;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        ScopedSpan span = tracer != null ? tracer.startScopedSpan("auth.registration") : null;
        if (span != null) {
            span.tag("service.operation", "register");
            span.tag("event.type", "UserRegisteredEvent");
        }
        try {
            String normalizedEmail = request.getEmail().toLowerCase().trim();

            if (userCredentialRepository.existsByEmail(normalizedEmail)) {
                throw new EmailAlreadyExistsException("An account with this email already exists");
            }

            String hashedPassword = passwordEncoder.encode(request.getPassword());
            UserCredential credential = new UserCredential(normalizedEmail, hashedPassword, "USER");
            UserCredential savedCredential = userCredentialRepository.save(credential);

            outboxService.saveUserRegisteredOutboxEvent(savedCredential.getId());

            meterRegistry.counter("devsphere.auth.registration.total", "status", "success").increment();
            meterRegistry.counter("devsphere_authentication_attempts_total", "type", "registration", "status", "success").increment();

            return new RegisterResponse(
                    savedCredential.getId(),
                    savedCredential.getEmail(),
                    savedCredential.getCreatedAt()
            );
        } catch (Exception e) {
            if (span != null) {
                span.error(e);
            }
            meterRegistry.counter("devsphere.auth.registration.total", "status", "failure").increment();
            meterRegistry.counter("devsphere_authentication_attempts_total", "type", "registration", "status", "failure").increment();
            throw e;
        } finally {
            if (span != null) {
                span.end();
            }
        }
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        ScopedSpan span = tracer != null ? tracer.startScopedSpan("auth.login") : null;
        if (span != null) {
            span.tag("service.operation", "login");
        }
        try {
            String normalizedEmail = request.getEmail().toLowerCase().trim();

            UserCredential credential = userCredentialRepository.findByEmail(normalizedEmail)
                    .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

            if (!passwordEncoder.matches(request.getPassword(), credential.getPasswordHash())) {
                throw new InvalidCredentialsException("Invalid email or password");
            }

            String token = jwtService.generateToken(credential.getId(), credential.getEmail(), credential.getRole());
            meterRegistry.counter("devsphere.auth.login.total", "status", "success").increment();
            meterRegistry.counter("devsphere_authentication_attempts_total", "type", "login", "status", "success").increment();
            return new LoginResponse(token, jwtService.getExpirationSeconds());
        } catch (Exception e) {
            if (span != null) {
                span.error(e);
            }
            meterRegistry.counter("devsphere.auth.login.total", "status", "failure").increment();
            meterRegistry.counter("devsphere_authentication_attempts_total", "type", "login", "status", "failure").increment();
            throw e;
        } finally {
            if (span != null) {
                span.end();
            }
        }
    }
}

