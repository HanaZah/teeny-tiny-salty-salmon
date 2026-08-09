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
import org.springframework.test.web.servlet.MockMvc;
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

    private User testUser;

    @BeforeAll
    void setUpAll() {
        userRepository.deleteAll();

        testUser = TestFixtureFactory.createIntegrationUser(
                200L, "IT-USR-1", "hash", UserType.ADVISOR
        );
        testUser = userRepository.save(testUser);
    }

    @Test
    void getCurrentUser_Authenticated_Returns200AndProfile() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                .with(jwt()
                        .jwt(j -> j.subject(testUser.getEmployeeId()))
                        .authorities(new SimpleGrantedAuthority("ADVISOR"))
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId").value(testUser.getEmployeeId()))
                .andExpect(jsonPath("$.firstName").value(testUser.getFirstName()));
    }

    @Test
    void getCurrentUser_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateProfile_ValidPayload_Returns200AndUpdatedProfile() throws Exception {
        Integer currentVersion = userRepository.findByEmployeeId(testUser.getEmployeeId()).orElseThrow().getVersion();

        String updatedFirst = "NewFirst";
        String updatedEmail = "new@finadvise.com";
        UserUpdateDTO request = new UserUpdateDTO(
                currentVersion, updatedFirst, "NewLast", "+420999888777", updatedEmail
        );

        mockMvc.perform(put("/api/v1/users/me/profile")
                        .with(jwt()
                                .jwt(j -> j.subject(testUser.getEmployeeId()))
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
                null, "NewFirst", "NewLast", "+420999888777", "new@finadvise.com"
        );

        mockMvc.perform(put("/api/v1/users/me/profile")
                        .with(jwt()
                                .jwt(j -> j.subject(testUser.getEmployeeId()))
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
        Integer currentVersion = userRepository.findByEmployeeId(testUser.getEmployeeId()).orElseThrow().getVersion();

        UserUpdateDTO request = new UserUpdateDTO(
                currentVersion, "H@cker!", "Name", "+420999888777", "new@finadvise.com"
        );

        mockMvc.perform(put("/api/v1/users/me/profile")
                        .with(jwt()
                                .jwt(j -> j.subject(testUser.getEmployeeId()))
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
                999, "NewFirst", "NewLast", "+420999888777", "new@finadvise.com"
        );

        mockMvc.perform(put("/api/v1/users/me/profile")
                        .with(jwt()
                                .jwt(j -> j.subject(testUser.getEmployeeId()))
                                .authorities(new SimpleGrantedAuthority("ADVISOR"))
                        )
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value(ErrorCodes.VERSION_MISMATCH));
    }
}