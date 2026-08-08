package com.finadvise.crm.users;

import com.finadvise.crm.AbstractIntegrationTest;
import com.finadvise.crm.TestFixtureFactory;
import com.finadvise.crm.common.RandomSecureStringGenerator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AuthServiceIT extends AbstractIntegrationTest {
    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RandomSecureStringGenerator randomStringGenerator;

    private User testUser;
    private final String RAW_PASSWORD = "RealPassword123!";

    @BeforeAll
    void setUpAll() {
        userRepository.deleteAll();
        String encodedPassword = passwordEncoder.encode(RAW_PASSWORD);

        testUser = TestFixtureFactory.createIntegrationUser(
                        998L,
                        "IT-EMP-1",
                        encodedPassword,
                        UserType.ADVISOR,
                        randomStringGenerator.generateRandomNumeric(8)
                );

        userRepository.save(testUser);
    }

    @Test
    void authenticateAndGenerateToken_StandardUser_ReturnsDtoWithAdvisorAuthority() {
        LoginRequestDTO request = new LoginRequestDTO(testUser.getEmployeeId(), RAW_PASSWORD);

        AuthResponseDTO response = authService.authenticateAndGenerateToken(request);

        assertNotNull(response);
        assertNotNull(response.accessToken());
        assertEquals("Bearer", response.tokenType());
    }

    @Test
    void authenticateAndGenerateToken_InvalidPassword_ThrowsBadCredentials() {
        LoginRequestDTO request = new LoginRequestDTO(testUser.getEmployeeId(), "WrongPassword!");

        assertThrows(BadCredentialsException.class, () ->
                authService.authenticateAndGenerateToken(request)
        );
    }
}
