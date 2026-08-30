package com.elrecetariodeshir.backend.admin.auth;

import java.util.Locale;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private final AdminBootstrapProperties properties;
    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminBootstrapRunner(
            AdminBootstrapProperties properties,
            AdminUserRepository adminUserRepository,
            PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            return;
        }

        String username = normalizeUsername(properties.getUsername());
        String password = properties.getPassword();

        if (username == null || password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "Admin bootstrap requires username and password.");
        }

        if (adminUserRepository.existsByUsernameIgnoreCase(username)) {
            return;
        }

        adminUserRepository.save(
                new AdminUser(
                        username,
                        passwordEncoder.encode(password),
                        true));
    }

    private String normalizeUsername(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }

        return username.trim().toLowerCase(Locale.ROOT);
    }
}
