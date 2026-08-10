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
import tools.jackson.databind.ObjectMapper;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserAdministrationControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testAdmin;
    private User testAdvisor;

    @BeforeAll
    void setUpAll() {
        cleanDatabase();

        testAdmin = TestFixtureFactory.createIntegrationAdmin(
                501L, "IT-ADM-1", passwordEncoder.encode("hash")
        );
        userRepository.save(testAdmin);

        testAdvisor = TestFixtureFactory.createIntegrationUser(
                502L, "IT-ADV-1", passwordEncoder.encode("hash"), UserType.ADVISOR
        );
        testAdvisor.setFirstName("SearchableName");
        userRepository.save(testAdvisor);
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor adminJwt() {
        return jwt().jwt(j -> j.subject(testAdmin.getEmployeeId())).authorities(new SimpleGrantedAuthority("ADMIN"));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor advisorJwt() {
        return jwt().jwt(j -> j.subject(testAdvisor.getEmployeeId())).authorities(new SimpleGrantedAuthority("ADVISOR"));
    }

    // --- GLOBAL SECURITY CONSTRAINTS ---

    @Test
    void globalSecurity_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users/" + testAdvisor.getEmployeeId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void globalSecurity_AdvisorToken_Returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users/" + testAdvisor.getEmployeeId())
                        .with(advisorJwt()))
                .andExpect(status().isForbidden());
    }

    // --- GET USER DETAIL ---

    @Test
    void getUserDetail_ValidId_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users/" + testAdvisor.getEmployeeId())
                        .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId").value(testAdvisor.getEmployeeId()))
                .andExpect(jsonPath("$.email").value(testAdvisor.getEmail()));
    }

    @Test
    void getUserDetail_InvalidId_Returns404() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users/UNKNOWN")
                        .with(adminJwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value(ErrorCodes.RESOURCE_NOT_FOUND));
    }

    // --- GET ORPHANED PORTFOLIOS ---

    @Test
    void getOrphanedPortfolios_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users/orphaned-portfolios")
                        .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").isNumber());
    }

    // --- SEARCH USERS ---

    @Test
    void searchUsers_ValidParams_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .param("name", "SearchableName")
                        .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].employeeId").value(testAdvisor.getEmployeeId()))
                .andExpect(jsonPath("$.content[0].firstName").value(testAdvisor.getFirstName()));
    }

    // --- REGISTER ADVISOR ---

    @Test
    void registerAdvisor_ValidPayload_Returns201() throws Exception {
        UserCreateDTO request = new UserCreateDTO(
                "88887777", "New", "Guy", "+420111222333", "new.guy@finadvise.com"
        );

        mockMvc.perform(post("/api/v1/admin/users/register")
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employeeId").exists())
                .andExpect(jsonPath("$.email").value("new.guy@finadvise.com"))
                .andExpect(jsonPath("$.userType").value("ADVISOR"));
    }

    @Test
    void registerAdvisor_InvalidPayload_Returns400() throws Exception {
        UserCreateDTO request = new UserCreateDTO(
                "123", "", "Guy", "+420111222333", "invalid-email"
        );

        mockMvc.perform(post("/api/v1/admin/users/register")
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(ErrorCodes.VALIDATION_FAILED))
                .andExpect(jsonPath("$.invalid_params.ico").exists())
                .andExpect(jsonPath("$.invalid_params.email").exists());
    }

    @Test
    void registerAdvisor_DuplicateIco_Returns409() throws Exception {
        UserCreateDTO request = new UserCreateDTO(
                testAdvisor.getIco(), "Copy", "Cat", "+420111222333", "copy@finadvise.com"
        );

        mockMvc.perform(post("/api/v1/admin/users/register")
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value(ErrorCodes.RESOURCE_CONFLICT));
    }

    // --- UPDATE STATUS ---

    @Test
    void updateUserStatus_ValidPayload_Returns200() throws Exception {
        UserStatusUpdateDTO request = new UserStatusUpdateDTO(false);

        mockMvc.perform(patch("/api/v1/admin/users/" + testAdvisor.getEmployeeId() + "/status")
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));

        // Revert status to true
        UserStatusUpdateDTO revertRequest = new UserStatusUpdateDTO(true);
        mockMvc.perform(patch("/api/v1/admin/users/" + testAdvisor.getEmployeeId() + "/status")
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(revertRequest)))
                .andExpect(status().isOk());
    }

    @Test
    void updateUserStatus_MissingIsActive_Returns400() throws Exception {
        UserStatusUpdateDTO request = new UserStatusUpdateDTO(null);

        mockMvc.perform(patch("/api/v1/admin/users/" + testAdvisor.getEmployeeId() + "/status")
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(ErrorCodes.VALIDATION_FAILED))
                .andExpect(jsonPath("$.invalid_params.isActive").exists());
    }

    @Test
    void updateUserStatus_AdminTarget_Returns400() throws Exception {
        UserStatusUpdateDTO request = new UserStatusUpdateDTO(false);

        mockMvc.perform(patch("/api/v1/admin/users/" + testAdmin.getEmployeeId() + "/status")
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(ErrorCodes.INVALID_INPUT_VALUE));
    }

    // --- RESET PASSWORD ---

    @Test
    void resetPassword_ValidId_Returns200() throws Exception {
        mockMvc.perform(post("/api/v1/admin/users/" + testAdvisor.getEmployeeId() + "/reset-password")
                        .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId").value(testAdvisor.getEmployeeId()));
    }

    @Test
    void resetPassword_InvalidId_Returns404() throws Exception {
        mockMvc.perform(post("/api/v1/admin/users/UNKNOWN/reset-password")
                        .with(adminJwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value(ErrorCodes.RESOURCE_NOT_FOUND));
    }

    // --- ADVISOR SUGGESTIONS ---

    @Test
    void getAdvisorSuggestions_ValidParams_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users/advisors-suggestions")
                        .param("name", "Searchable")
                        .param("limit", "10")
                        .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].employeeId").value(testAdvisor.getEmployeeId()))
                .andExpect(jsonPath("$[0].fullName").exists());
    }

    @Test
    void getAdvisorSuggestions_InvalidLimit_Returns400() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users/advisors-suggestions")
                        .param("name", "Test")
                        .param("limit", "1000") // Exceeds max 100
                        .with(adminJwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(ErrorCodes.VALIDATION_FAILED))
                .andExpect(jsonPath("$.invalid_params.limit").exists());
    }

    // --- UPDATE EMAIL ---

    @Test
    void updateUserEmail_ValidPayload_Returns200() throws Exception {
        Integer currentVersion = userRepository.findByEmployeeId(testAdvisor.getEmployeeId()).orElseThrow().getVersion();
        UserEmailUpdateDTO request = new UserEmailUpdateDTO(currentVersion, "updated.e2e@finadvise.com");

        mockMvc.perform(patch("/api/v1/admin/users/" + testAdvisor.getEmployeeId() + "/email")
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("updated.e2e@finadvise.com"));
    }

    @Test
    void updateUserEmail_InvalidFormat_Returns400() throws Exception {
        Integer currentVersion = userRepository.findByEmployeeId(testAdvisor.getEmployeeId()).orElseThrow().getVersion();
        UserEmailUpdateDTO request = new UserEmailUpdateDTO(currentVersion, "not-an-email");

        mockMvc.perform(patch("/api/v1/admin/users/" + testAdvisor.getEmployeeId() + "/email")
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(ErrorCodes.VALIDATION_FAILED))
                .andExpect(jsonPath("$.invalid_params.email").exists());
    }

    @Test
    void updateUserEmail_VersionMismatch_Returns409() throws Exception {
        UserEmailUpdateDTO request = new UserEmailUpdateDTO(999, "valid.email@finadvise.com");

        mockMvc.perform(patch("/api/v1/admin/users/" + testAdvisor.getEmployeeId() + "/email")
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value(ErrorCodes.VERSION_MISMATCH));
    }
}