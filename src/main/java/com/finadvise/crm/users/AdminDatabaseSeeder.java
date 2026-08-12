package com.finadvise.crm.users;

import com.finadvise.crm.common.ObfuscatedIdGenerator;
import com.finadvise.crm.config.AdminProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
class AdminDatabaseSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObfuscatedIdGenerator idGenerator;
    private final AdminProperties adminProperties;

    @Override
    @Transactional
    public void run(String @NonNull ... args) {
        if (userRepository.findFirstActiveByUserType_Admin().isEmpty()) {
            Long userId = userRepository.getNextSequenceValue();
            String employeeId = idGenerator.encode(userId);

            User admin = User.builder()
                    .id(userId)
                    .employeeId(employeeId)
                    .passwordHash(passwordEncoder.encode(adminProperties.password()))
                    .firstName(adminProperties.firstName())
                    .lastName(adminProperties.lastName())
                    .phone(adminProperties.phone())
                    .email(adminProperties.email())
                    .userType(UserType.ADMIN)
                    .build();

            userRepository.save(admin);
            log.info("Seeded initial Admin user with Employee ID: {}", employeeId);
        }
    }
}