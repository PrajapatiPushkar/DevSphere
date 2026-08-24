package com.devsphere.auth.config;

import com.devsphere.auth.entity.UserCredential;
import com.devsphere.auth.repository.UserCredentialRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminUserInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminUserInitializer.class);

    private final UserCredentialRepository userCredentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final boolean seedAdmin;
    private final String adminEmail;
    private final String adminPassword;

    public AdminUserInitializer(
            UserCredentialRepository userCredentialRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.security.seed-admin:true}") boolean seedAdmin,
            @Value("${app.security.admin-email:admin@devsphere.local}") String adminEmail,
            @Value("${app.security.admin-password:AdminPassword123!}") String adminPassword) {
        this.userCredentialRepository = userCredentialRepository;
        this.passwordEncoder = passwordEncoder;
        this.seedAdmin = seedAdmin;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (!seedAdmin) {
            return;
        }
        String normalizedEmail = adminEmail.toLowerCase().trim();
        if (!userCredentialRepository.existsByEmail(normalizedEmail)) {
            String hashedPassword = passwordEncoder.encode(adminPassword);
            UserCredential admin = new UserCredential(normalizedEmail, hashedPassword, "ADMIN");
            userCredentialRepository.save(admin);
            log.info("Seeded initial ADMIN account: {}", normalizedEmail);
        }
    }
}
