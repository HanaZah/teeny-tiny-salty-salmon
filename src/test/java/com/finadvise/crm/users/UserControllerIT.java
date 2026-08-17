package com.finadvise.crm.users;

import com.finadvise.crm.AbstractIntegrationTest;
import com.finadvise.crm.TestFixtureFactory;
import com.finadvise.crm.common.ErrorCodes;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private User testUser1;
    private User testUser2;
    
    private final String testPassword = "secretPassword123!";

    @BeforeAll
    void setUpAll() {
        cleanDatabase();

        String encodedPassword = passwordEncoder.encode(testPassword);

        testUser1 = TestFixtureFactory.createIntegrationUser(
                200L, "IT-USR-1", encodedPassword, UserType.ADVISOR
        );
        testUser1 = userRepository.save(testUser1);

        testUser2 = TestFixtureFactory.createIntegrationUser(
                201L, "IT-USR-2", encodedPassword, UserType.ADVISOR
        );
        testUser2 = userRepository.save(testUser2);
    }

    // --- 1. GET CURRENT USER ---

    @Test
    void getCurrentUser_Authenticated_Returns200AndProfile() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .with(jwt()
                                .jwt(j -> j.subject(testUser1.getEmployeeId()))
                                .authorities(new SimpleGrantedAuthority("ADVISOR"))
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId").value(testUser1.getEmployeeId()))
                .andExpect(jsonPath("$.firstName").value(testUser1.getFirstName()));
    }

    @Test
    void getCurrentUser_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }

    // --- 2. UPDATE PROFILE ---

    @Test
    void updateProfile_ValidPayload_Returns200AndUpdatedProfile() throws Exception {
        Integer currentVersion = userRepository.findByEmployeeId(testUser1.getEmployeeId()).orElseThrow().getVersion();

        String updatedFirst = "NewFirst";
        String updatedEmail = "new@finadvise.com";
        UserUpdateDTO request = new UserUpdateDTO(
                currentVersion, testUser1.getIco(), updatedFirst, "NewLast", "+420999888777", updatedEmail
        );

        mockMvc.perform(put("/api/v1/users/me/profile")
                        .with(jwt()
                                .jwt(j -> j.subject(testUser1.getEmployeeId()))
                                .authorities(new SimpleGrantedAuthority("ADVISOR"))
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value(updatedFirst))
                .andExpect(jsonPath("$.email").value(updatedEmail));
    }

    @Test
    void updateProfile_MissingVersion_Returns400() throws Exception {
        UserUpdateDTO request = new UserUpdateDTO(
                null, testUser1.getIco(), "NewFirst", "NewLast", "+420999888777", "new@finadvise.com"
        );

        mockMvc.perform(put("/api/v1/users/me/profile")
                        .with(jwt()
                                .jwt(j -> j.subject(testUser1.getEmployeeId()))
                                .authorities(new SimpleGrantedAuthority("ADVISOR"))
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(ErrorCodes.VALIDATION_FAILED))
                .andExpect(jsonPath("$.invalid_params.version").exists());
    }

    @Test
    void updateProfile_InvalidNameCharacters_Returns400() throws Exception {
        Integer currentVersion = userRepository.findByEmployeeId(testUser1.getEmployeeId()).orElseThrow().getVersion();

        UserUpdateDTO request = new UserUpdateDTO(
                currentVersion, testUser1.getIco(), "H@cker!", "Name", "+420999888777", "new@finadvise.com"
        );

        mockMvc.perform(put("/api/v1/users/me/profile")
                        .with(jwt()
                                .jwt(j -> j.subject(testUser1.getEmployeeId()))
                                .authorities(new SimpleGrantedAuthority("ADVISOR"))
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.invalid_params.firstName").exists());
    }

    @Test
    void updateProfile_VersionMismatch_Returns409() throws Exception {
        UserUpdateDTO request = new UserUpdateDTO(
                999, testUser1.getIco(), "NewFirst", "NewLast", "+420999888777", "new@finadvise.com"
        );

        mockMvc.perform(put("/api/v1/users/me/profile")
                        .with(jwt()
                                .jwt(j -> j.subject(testUser1.getEmployeeId()))
                                .authorities(new SimpleGrantedAuthority("ADVISOR"))
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value(ErrorCodes.VERSION_MISMATCH));
    }

    @Test
    void updateProfile_DuplicitIco_Returns409() throws Exception {
        UserUpdateDTO request = new UserUpdateDTO(
                999, testUser2.getIco(), "NewFirst", "NewLast", "+420999888777", "new@finadvise.com"
        );

        mockMvc.perform(put("/api/v1/users/me/profile")
                        .with(jwt()
                                .jwt(j -> j.subject(testUser1.getEmployeeId()))
                                .authorities(new SimpleGrantedAuthority("ADVISOR"))
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value(ErrorCodes.VERSION_MISMATCH));
    }

    // --- 3. CHANGE PASSWORD ---

    @Test
    void changePassword_HappyPath_Returns200() throws Exception {
        PasswordChangeDTO request = new PasswordChangeDTO(testPassword, "NewStr0ngP@ss1!");

        mockMvc.perform(put("/api/v1/users/me/password")
                        .with(jwt()
                                .jwt(j -> j.subject(testUser1.getEmployeeId()))
                                .authorities(new SimpleGrantedAuthority("ADVISOR"))
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId").value(testUser1.getEmployeeId()));

        // Cleanup: Revert the password to preserve the shared setup state
        transactionTemplate.executeWithoutResult(status -> {
            userRepository.forceUpdatePassword(testUser1.getEmployeeId(), passwordEncoder.encode(testPassword));
        });
    }

    @Test
    void changePassword_IncorrectCurrentPassword_Returns400() throws Exception {
        PasswordChangeDTO request = new PasswordChangeDTO("wrong-secret", "NewStr0ngP@ss1!");

        mockMvc.perform(put("/api/v1/users/me/password")
                        .with(jwt()
                                .jwt(j -> j.subject(testUser1.getEmployeeId()))
                                .authorities(new SimpleGrantedAuthority("ADVISOR"))
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(ErrorCodes.INVALID_INPUT_VALUE));
    }

    @Test
    void changePassword_SamePasswordConflict_Returns409() throws Exception {
        PasswordChangeDTO request = new PasswordChangeDTO(testPassword, testPassword);

        mockMvc.perform(put("/api/v1/users/me/password")
                        .with(jwt()
                                .jwt(j -> j.subject(testUser1.getEmployeeId()))
                                .authorities(new SimpleGrantedAuthority("ADVISOR"))
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value(ErrorCodes.RESOURCE_CONFLICT));
    }

    @Test
    void changePassword_ValidationFailure_Returns400() throws Exception {
        PasswordChangeDTO request = new PasswordChangeDTO(testPassword, "");

        mockMvc.perform(put("/api/v1/users/me/password")
                        .with(jwt()
                                .jwt(j -> j.subject(testUser1.getEmployeeId()))
                                .authorities(new SimpleGrantedAuthority("ADVISOR"))
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(ErrorCodes.VALIDATION_FAILED));
    }
}