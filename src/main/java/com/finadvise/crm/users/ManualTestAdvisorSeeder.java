package com.finadvise.crm.users;

import com.finadvise.crm.config.ManualTestAdvisorProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!prod")
public class ManualTestAdvisorSeeder implements CommandLineRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ManualTestAdvisorProperties advisorProperties;


    @Override
    public void run(String @NonNull ... args) {

        if (!userRepository.existsByEmployeeId(advisorProperties.employeeId())) {
            Long userId = userRepository.getNextSequenceValue();

            User advisor = User.builder()
                    .id(userId)
                    .ico("00000000")
                    .employeeId(advisorProperties.employeeId())
                    .passwordHash(passwordEncoder.encode(advisorProperties.password()))
                    .firstName(advisorProperties.firstName())
                    .lastName(advisorProperties.lastName())
                    .phone(advisorProperties.phone())
                    .email(advisorProperties.email())
                    .userType(UserType.ADVISOR)
                    .build();

            userRepository.save(advisor);
            log.info("Seeded Advisor user for manual testing with Employee ID: {}", advisorProperties.employeeId());
        }
    }
}
