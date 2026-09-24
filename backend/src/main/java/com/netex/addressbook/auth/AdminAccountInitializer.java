package com.netex.addressbook.auth;

import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminAccountInitializer implements ApplicationRunner {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public AdminAccountInitializer(UserRepository users, PasswordEncoder passwordEncoder,
            @Value("${ADMIN_EMAIL:}") String adminEmail,
            @Value("${ADMIN_PASSWORD:}") String adminPassword) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        if (adminEmail.isBlank() && adminPassword.isBlank()) {
            return;
        }
        String email = EmailAddress.normalize(adminEmail);
        if (email.isBlank() || email.length() > 254 || !email.contains("@")
                || adminPassword.length() < 8
                || adminPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException("Set a valid ADMIN_EMAIL and ADMIN_PASSWORD (8-72 bytes)");
        }
        users.findByEmail(email).ifPresentOrElse(existing -> {
            if (!existing.role().equals("ADMIN")) {
                throw new IllegalStateException("ADMIN_EMAIL already belongs to a regular user");
            }
        }, () -> users.create(email, passwordEncoder.encode(adminPassword), "ADMIN"));
    }
}
