package com.serviceflow.serviceflowos;

import com.serviceflow.serviceflowos.domain.Role;
import com.serviceflow.serviceflowos.domain.Tenant;
import com.serviceflow.serviceflowos.domain.TenantRepository;
import com.serviceflow.serviceflowos.domain.User;
import com.serviceflow.serviceflowos.domain.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Temporary, dev-only seed data so Day 3 has a real user to log in as.
 * Checkpoint 2, Day 4 replaces this with a versioned Flyway migration.
 */
@Component
public class DevDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);
    private static final String ADMIN_EMAIL = "admin@demo.serviceflow.os";
    private static final String ADMIN_PASSWORD = "admin123";

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DevDataSeeder(TenantRepository tenantRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.findByEmail(ADMIN_EMAIL).isPresent()) {
            return;
        }

        Tenant tenant = tenantRepository.findByName("Demo HVAC Co").orElseGet(() -> {
            Tenant t = new Tenant();
            t.setName("Demo HVAC Co");
            return tenantRepository.save(t);
        });

        User admin = new User();
        admin.setTenant(tenant);
        admin.setEmail(ADMIN_EMAIL);
        admin.setPasswordHash(passwordEncoder.encode(ADMIN_PASSWORD));
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);

        log.info("Seeded dev admin user -> email: {}, password: {}", ADMIN_EMAIL, ADMIN_PASSWORD);
    }
}
