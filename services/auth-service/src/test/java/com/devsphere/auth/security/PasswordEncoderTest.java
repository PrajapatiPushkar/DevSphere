package com.devsphere.auth.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class PasswordEncoderTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    @DisplayName("Should hash password securely using BCrypt and verify match")
    void passwordHashingAndVerification() {
        String rawPassword = "SecurePassword123";

        String hashedPassword = passwordEncoder.encode(rawPassword);

        assertThat(hashedPassword).isNotEqualTo(rawPassword);
        assertThat(hashedPassword).startsWith("$2a$").hasSize(60);
        assertThat(passwordEncoder.matches(rawPassword, hashedPassword)).isTrue();
        assertThat(passwordEncoder.matches("WrongPassword", hashedPassword)).isFalse();
    }
}
