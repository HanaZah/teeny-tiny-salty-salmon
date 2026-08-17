package com.finadvise.crm.users;

import com.finadvise.crm.common.SystemIntegrityException;
import com.finadvise.crm.config.AdminProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("admin-recovery")
@RequiredArgsConstructor
@Slf4j
@Order(2)
class AdminRecoverySeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminProperties adminProperties;
    private final ApplicationContext applicationContext;

    @Override
    @Transactional
    public void run(String @NonNull ... args) {
        User admin = userRepository.findFirstActiveByUserType_Admin()
                .orElseThrow(() -> new SystemIntegrityException("Cannot recover: Admin user does not exist yet."));

        admin.setPasswordHash(passwordEncoder.encode(adminProperties.password()));
        admin.setFirstName(adminProperties.firstName());
        admin.setLastName(adminProperties.lastName());
        admin.setPhone(adminProperties.phone());
        admin.setEmail(adminProperties.email());
        admin.setActive(true);

        userRepository.save(admin);
        log.warn("CRITICAL: Admin account was force-reset via 'admin-recovery' profile.");

        int exitCode = SpringApplication.exit(applicationContext, () -> 0);
        System.exit(exitCode);
    }
}