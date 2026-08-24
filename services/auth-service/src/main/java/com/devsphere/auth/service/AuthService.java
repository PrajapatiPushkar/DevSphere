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

    public AuthService(UserCredentialRepository userCredentialRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       OutboxService outboxService) {
        this(userCredentialRepository, passwordEncoder, jwtService, outboxService, new SimpleMeterRegistry());
    }

    @Autowired
    public AuthService(UserCredentialRepository userCredentialRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       OutboxService outboxService,
                       MeterRegistry meterRegistry) {
        this.userCredentialRepository = userCredentialRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.outboxService = outboxService;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        try {
            String normalizedEmail = request.getEmail().toLowerCase().trim();

            if (userCredentialRepository.existsByEmail(normalizedEmail)) {
                throw new EmailAlreadyExistsException("An account with this email already exists");
            }

            String hashedPassword = passwordEncoder.encode(request.getPassword());
            UserCredential credential = new UserCredential(normalizedEmail, hashedPassword);
            UserCredential savedCredential = userCredentialRepository.save(credential);

            outboxService.saveUserRegisteredOutboxEvent(savedCredential.getId());

            meterRegistry.counter("devsphere.auth.registration.total", "status", "success").increment();

            return new RegisterResponse(
                    savedCredential.getId(),
                    savedCredential.getEmail(),
                    savedCredential.getCreatedAt()
            );
        } catch (Exception e) {
            meterRegistry.counter("devsphere.auth.registration.total", "status", "failure").increment();
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        try {
            String normalizedEmail = request.getEmail().toLowerCase().trim();

            UserCredential credential = userCredentialRepository.findByEmail(normalizedEmail)
                    .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

            if (!passwordEncoder.matches(request.getPassword(), credential.getPasswordHash())) {
                throw new InvalidCredentialsException("Invalid email or password");
            }

            String token = jwtService.generateToken(credential.getId(), credential.getEmail());
            meterRegistry.counter("devsphere.auth.login.total", "status", "success").increment();
            return new LoginResponse(token, jwtService.getExpirationSeconds());
        } catch (Exception e) {
            meterRegistry.counter("devsphere.auth.login.total", "status", "failure").increment();
            throw e;
        }
    }
}
