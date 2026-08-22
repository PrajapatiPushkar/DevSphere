package com.devsphere.auth.service;

import com.devsphere.auth.dto.RegisterRequest;
import com.devsphere.auth.dto.RegisterResponse;
import com.devsphere.auth.entity.UserCredential;
import com.devsphere.auth.exception.EmailAlreadyExistsException;
import com.devsphere.auth.repository.UserCredentialRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserCredentialRepository userCredentialRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserCredentialRepository userCredentialRepository, PasswordEncoder passwordEncoder) {
        this.userCredentialRepository = userCredentialRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().toLowerCase().trim();

        if (userCredentialRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyExistsException("An account with this email already exists");
        }

        String hashedPassword = passwordEncoder.encode(request.getPassword());
        UserCredential credential = new UserCredential(normalizedEmail, hashedPassword);
        UserCredential savedCredential = userCredentialRepository.save(credential);

        return new RegisterResponse(
                savedCredential.getId(),
                savedCredential.getEmail(),
                savedCredential.getCreatedAt()
        );
    }
}
