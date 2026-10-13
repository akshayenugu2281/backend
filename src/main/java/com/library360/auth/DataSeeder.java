package com.library360.auth;

import com.library360.user.RoleRepository;
import com.library360.user.User;
import com.library360.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements ApplicationRunner {

    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;

    @Value("${app.seed.admin-email:}")
    private String email;

    @Value("${app.seed.admin-password:}")
    private String password;

    @Value("${app.seed.librarian-email:}")
    private String librarianEmail;

    @Value("${app.seed.librarian-password:}")
    private String librarianPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedAccount(email, password, "System Administrator", "ADMIN");
        seedAccount(librarianEmail, librarianPassword, "Library Manager", "LIBRARIAN");
    }

    private void seedAccount(String seedEmail, String seedPassword, String displayName, String roleName) {
        if (seedEmail == null || seedEmail.isBlank()) {
            return;
        }
        if (users.existsByEmail(seedEmail)) {
            return;
        }
        if (!validSeedPassword(seedPassword)) {
            log.warn("Initial {} was not created because the configured password is missing, is a template placeholder, or does not meet the password policy (at least 6 characters with both letters and numbers). Configure a unique password before first startup.", displayName);
            return;
        }

        log.info("Seeding initial {} user: {}", displayName, seedEmail);

        User staff = new User(seedEmail, encoder.encode(seedPassword), displayName);

        roles.findByName(roleName).ifPresent(role -> {
            staff.getRoles().add(role);
            users.save(staff);
            log.info("Initial {} user created successfully", displayName);
        });
    }

    private static boolean validSeedPassword(String seedPassword) {
        return seedPassword != null && !seedPassword.isBlank() && !seedPassword.startsWith("REPLACE_WITH_")
            && seedPassword.matches("(?=.*[A-Za-z])(?=.*[0-9]).{6,72}");
    }
}
