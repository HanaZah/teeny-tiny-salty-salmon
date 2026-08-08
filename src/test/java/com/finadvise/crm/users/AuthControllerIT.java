package com.finadvise.crm.users;

import com.finadvise.crm.AbstractIntegrationTest;
import com.finadvise.crm.TestFixtureFactory;
import com.finadvise.crm.common.ErrorCodes;
import com.finadvise.crm.common.RandomSecureStringGenerator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RandomSecureStringGenerator randomStringGenerator;

    private User testAdmin;
    @BeforeAll
    void setUpAll() {
        userRepository.deleteAll();
        User testUser = TestFixtureFactory.createIntegrationUser(
                888L,
                "IT-EMP-1",
                passwordEncoder.encode("E2EPassword123!"),
                UserType.ADVISOR,
                randomStringGenerator.generateRandomNumeric(8)
        );

        userRepository.save(testUser);

        testAdmin = TestFixtureFactory.createIntegrationAdmin(
               999L,
                "IT-ADMIN",
                passwordEncoder.encode("E2EPassword123!")
        );

        userRepository.save(testAdmin);
    }

    //----E2E LOGIN TESTS----

    @Test
    void login_ValidCredentials_Returns200AndToken() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO("IT-EMP-1", "E2EPassword123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void login_InvalidCredentials_Returns401ProblemDetail() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO("IT-EMP-1", "BadPassword");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value(ErrorCodes.BAD_CREDENTIALS))
                .andExpect(jsonPath("$.title").value("Authentication Failed"));
    }

    @Test
    void login_MissingEmployeeId_Returns400WithValidationErrors() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO("", "E2EPassword123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.type").value(ErrorCodes.VALIDATION_FAILED))
                .andExpect(jsonPath("$.invalid_params.employeeId").exists());
    }

    @Test
    void login_InvalidEmployeeIdPattern_Returns400WithValidationErrors() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO("INVALID_ID_@", "E2EPassword123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.type").value(ErrorCodes.VALIDATION_FAILED))
                .andExpect(jsonPath("$.invalid_params.employeeId").value("Employee ID has wrong size or pattern"));
    }

    @Test
    void login_EmployeeIdExceedsMaxLength_Returns400WithValidationErrors() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO("THIS-ID-IS-WAY-TOO-LONG-TO-BE-VALID", "E2EPassword123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.invalid_params.employeeId").exists());
    }

    @Test
    void login_PasswordTooShort_Returns400WithValidationErrors() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO("IT-EMP-1", "short");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.type").value(ErrorCodes.VALIDATION_FAILED))
                .andExpect(jsonPath("$.invalid_params.password").value("Password must be between 8 and 72 characters"));
    }

    //----E2E FORGOTTEN PASSWORD TESTS----

    @Test
    void forgotPassword_AdminExists_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/auth/forgotten-password"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value(testAdmin.getFirstName()))
                .andExpect(jsonPath("$.lastName").value(testAdmin.getLastName()))
                .andExpect(jsonPath("$.email").value(testAdmin.getEmail()))
                .andExpect(jsonPath("$.phone").value(testAdmin.getPhone()));
    }

    @Test
    void forgotPassword_AdminNotFound_Returns500() throws Exception {
        testAdmin = userRepository.findById(testAdmin.getId()).orElseThrow(); // get current version
        testAdmin.setActive(false);
        userRepository.save(testAdmin);

        mockMvc.perform(get("/api/v1/auth/forgotten-password"))
                .andExpect(status().is5xxServerError());

        // reset admin status
        testAdmin = userRepository.findById(testAdmin.getId()).orElseThrow();
        testAdmin.setActive(true);
        userRepository.save(testAdmin);
    }
}