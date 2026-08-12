package com.finadvise.crm.clients;

import com.finadvise.crm.AbstractIntegrationTest;
import com.finadvise.crm.TestFixtureFactory;
import com.finadvise.crm.addresses.Address;
import com.finadvise.crm.addresses.AddressInputDTO;
import com.finadvise.crm.common.ErrorCodes;
import com.finadvise.crm.users.User;
import com.finadvise.crm.users.UserType;
import jakarta.persistence.EntityManager;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.Objects;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClientControllerIT extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private EntityManager entityManager;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private ClientRepository clientRepository;

    private User testAdmin;
    private User testAdvisor1;
    private User testAdvisor2;
    private User emptyAdvisor; // Advisor with 0 clients
    private User createTestAdvisor;
    private Client testClient1;
    private Client testClient2;
    private Client updateClient;

    @BeforeAll
    void setUpAll() {
        transactionTemplate.executeWithoutResult(status -> {
            cleanDatabase();
            String hash = passwordEncoder.encode("secret");

            Address testAddress = TestFixtureFactory.createIntegrationAddress(901);
            entityManager.persist(testAddress);

            testAdmin = TestFixtureFactory.createIntegrationAdmin(901L, "IT-CTRL-ADM1", hash);
            testAdvisor1 = TestFixtureFactory.createIntegrationUser(902L, "IT-CTRL-ADV1", hash, UserType.ADVISOR);
            testAdvisor2 = TestFixtureFactory.createIntegrationUser(903L, "IT-CTRL-ADV2", hash, UserType.ADVISOR);
            emptyAdvisor = TestFixtureFactory.createIntegrationUser(904L, "IT-CTRL-ADV3", hash, UserType.ADVISOR);
            createTestAdvisor = TestFixtureFactory.createIntegrationUser(905L, "IT-CTRL-ADV4", hash, UserType.ADVISOR);

            entityManager.persist(testAdmin);
            entityManager.persist(testAdvisor1);
            entityManager.persist(testAdvisor2);
            entityManager.persist(emptyAdvisor);
            entityManager.persist(createTestAdvisor);

            testClient1 = TestFixtureFactory.createIntegrationClient(901L, "UID-CTRL-C1", testAdvisor1, testAddress);
            testClient1.setFirstName("John");
            testClient1.setLastName("Doe");
            entityManager.persist(testClient1);

            testClient2 = TestFixtureFactory.createIntegrationClient(902L, "UID-CTRL-C2", testAdvisor2, testAddress);
            entityManager.persist(testClient2);

            updateClient = TestFixtureFactory.createIntegrationClient(903L, "UID-CTRL-C3", testAdvisor1, testAddress);
            entityManager.persist(updateClient);

            entityManager.flush();
        });
    }

    private RequestPostProcessor adminJwt() {
        return jwt().jwt(j -> j.subject(testAdmin.getEmployeeId())).authorities(new SimpleGrantedAuthority("ADMIN"));
    }

    private RequestPostProcessor advisor1Jwt() {
        return jwt().jwt(j -> j.subject(testAdvisor1.getEmployeeId())).authorities(new SimpleGrantedAuthority("ADVISOR"));
    }

    private RequestPostProcessor emptyAdvisorJwt() {
        return jwt().jwt(j -> j.subject(emptyAdvisor.getEmployeeId())).authorities(new SimpleGrantedAuthority("ADVISOR"));
    }

    private RequestPostProcessor createTestAdvisorJwt() {
        return jwt().jwt(j -> j.subject(createTestAdvisor.getEmployeeId())).authorities(new SimpleGrantedAuthority("ADVISOR"));
    }

    private AddressInputDTO getValidAddressInput() {
        return new AddressInputDTO("Test Street", "123/1a", "Test City", "123 45");
    }

    // --- 1. GLOBAL SECURITY & SERIALIZATION ---

    @Test
    void globalSecurity_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/clients/" + testClient1.getClientUid()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void globalSecurity_MalformedJson_Returns400() throws Exception {
        mockMvc.perform(post("/api/v1/clients/search")
                        .with(advisor1Jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"Doe\", }")) // Trailing comma syntax error
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request")); // Default spring problem detail for HTTP message not readable
    }

    // --- 2. GET CLIENT DETAIL ---

    @Test
    void getClientDetail_HappyPath_Advisor_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/clients/" + testClient1.getClientUid())
                        .with(advisor1Jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientUid").value(testClient1.getClientUid()))
                .andExpect(jsonPath("$.budget").exists())
                .andExpect(jsonPath("$.permanentAddress").exists());
    }

    @Test
    void getClientDetail_HappyPath_Admin_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/clients/" + testClient1.getClientUid())
                        .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientUid").value(testClient1.getClientUid()));
    }

    @Test
    void getClientDetail_CrossAdvisorIsolation_Returns404() throws Exception {
        mockMvc.perform(get("/api/v1/clients/" + testClient2.getClientUid()) // Owned by Adv 2
                        .with(advisor1Jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value(ErrorCodes.RESOURCE_NOT_FOUND));
    }

    // --- 3. GET RECENT OVERVIEWS ---

    @Test
    void getRecentOverviews_HappyPath_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/clients/recent?limit=5")
                        .with(advisor1Jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                // checks that the array contains our target client, regardless of index
                .andExpect(jsonPath("$[?(@.clientUid == '" + testClient1.getClientUid() + "')]").exists());
    }

    @Test
    void getRecentOverviews_EmptyState_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/clients/recent?limit=5")
                        .with(emptyAdvisorJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty()); // Empty array []
    }

    @Test
    void getRecentOverviews_LimitTooLow_Returns400() throws Exception {
        mockMvc.perform(get("/api/v1/clients/recent?limit=0")
                        .with(advisor1Jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(ErrorCodes.VALIDATION_FAILED));
    }

    @Test
    void getRecentOverviews_LimitTooHigh_Returns400() throws Exception {
        mockMvc.perform(get("/api/v1/clients/recent?limit=21")
                        .with(advisor1Jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(ErrorCodes.VALIDATION_FAILED));
    }

    @Test
    void getRecentOverviews_AdminDenied_Returns403() throws Exception {
        mockMvc.perform(get("/api/v1/clients/recent?limit=5")
                        .with(adminJwt()))
                .andExpect(status().isForbidden());
    }

    // --- 4. GET SUGGESTIONS ---

    @Test
    void getSuggestions_HappyPath_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/clients/suggestions?name=Doe&limit=10")
                        .with(advisor1Jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].clientUid").value(testClient1.getClientUid()));
    }

    @Test
    void getSuggestions_NullName_Returns200() throws Exception {
        // Omitting 'name' param
        mockMvc.perform(get("/api/v1/clients/suggestions?limit=10")
                        .with(advisor1Jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getSuggestions_MalformedQuery_Returns400() throws Exception {
        mockMvc.perform(get("/api/v1/clients/suggestions?name=<script>&limit=10")
                        .with(advisor1Jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(ErrorCodes.VALIDATION_FAILED))
                .andExpect(jsonPath("$.invalid_params.name").exists());
    }

    // --- 5. POST SEARCH ---

    @Test
    void searchClients_HappyPath_ComplexSearch_Returns200() throws Exception {
        ClientSearchCriteriaDTO criteria = new ClientSearchCriteriaDTO(null, "Doe", null, null, null);
        mockMvc.perform(post("/api/v1/clients/search")
                        .with(advisor1Jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(criteria)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    @Test
    void searchClients_EmptyPayload_Returns200() throws Exception {
        mockMvc.perform(post("/api/v1/clients/search")
                        .with(advisor1Jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void searchClients_InvalidPattern_Returns400() throws Exception {
        ClientSearchCriteriaDTO criteria = new ClientSearchCriteriaDTO("%HACK%", null, "123456789012", null, null);
        mockMvc.perform(post("/api/v1/clients/search")
                        .with(advisor1Jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(criteria)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(ErrorCodes.VALIDATION_FAILED))
                .andExpect(jsonPath("$.invalid_params.advisorEmployeeId").exists())
                .andExpect(jsonPath("$.invalid_params.personalId").exists());
    }

    // --- 6. POST CREATE CLIENT ---

    @Test
    void createClient_HappyPath_Returns201AndLocationHeader() throws Exception {
        ClientCreateDTO dto = new ClientCreateDTO(
                "8801019991", LocalDate.of(1988, 1, 1), "New", "User", "IT",
                "+420111222333", "new@finadvise.com", "123456789", LocalDate.now().minusYears(1),
                LocalDate.now().plusYears(9), "MV CR", getValidAddressInput(), getValidAddressInput()
        );

        mockMvc.perform(post("/api/v1/clients")
                        .with(createTestAdvisorJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.clientUid").exists());
    }

    @Test
    void createClient_ValidationEdgeCase_LeapYear_Returns201() throws Exception {
        ClientCreateDTO dto = new ClientCreateDTO(
                "9202299992", LocalDate.of(1992, 2, 29), "Leap", "User", "IT",
                "+420111222333", "leap@finadvise.com", "987654321", LocalDate.now().minusYears(1),
                LocalDate.now().plusYears(9), "MV CR", getValidAddressInput(), getValidAddressInput()
        );

        mockMvc.perform(post("/api/v1/clients")
                        .with(createTestAdvisorJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());


    }

    @Test
    void createClient_ValidationFailure_CzechAddress_Returns400() throws Exception {
        AddressInputDTO badAddress = new AddressInputDTO("St", "0", "City", "12345"); // Bad house num & postal
        ClientCreateDTO dto = new ClientCreateDTO(
                "8801019993", LocalDate.of(1988, 1, 1), "New", "User", "IT",
                "+420111222333", "new@finadvise.com", "111222333", LocalDate.now().minusYears(1),
                LocalDate.now().plusYears(9), "MV CR", badAddress, badAddress
        );

        mockMvc.perform(post("/api/v1/clients")
                        .with(createTestAdvisorJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(ErrorCodes.VALIDATION_FAILED))
                .andExpect(jsonPath("$.invalid_params['residentialAddress.houseNumber']").exists())
                .andExpect(jsonPath("$.invalid_params['residentialAddress.postalCode']").exists());
    }

    @Test
    void createClient_ValidationFailure_EmailPhone_Returns400() throws Exception {
        ClientCreateDTO dto = new ClientCreateDTO(
                "8801019994", LocalDate.of(1988, 1, 1), "New", "User", "IT",
                "phone-letters", "plain-text-email", "222333444", LocalDate.now().minusYears(1),
                LocalDate.now().plusYears(9), "MV CR", getValidAddressInput(), getValidAddressInput()
        );

        mockMvc.perform(post("/api/v1/clients")
                        .with(createTestAdvisorJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.invalid_params.phone").exists())
                .andExpect(jsonPath("$.invalid_params.email").exists());
    }

    @Test
    void createClient_BusinessValidationMapping_Returns400() throws Exception {
        // Issue date in the future
        ClientCreateDTO dto = new ClientCreateDTO(
                "8801019995", LocalDate.of(1988, 1, 1), "New", "User", "IT",
                "+420", "a@b.c", "333444555", LocalDate.now().plusDays(10),
                LocalDate.now().plusYears(9), "MV CR", getValidAddressInput(), getValidAddressInput()
        );

        mockMvc.perform(post("/api/v1/clients")
                        .with(createTestAdvisorJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(ErrorCodes.INVALID_INPUT_VALUE)); // From GlobalExceptionHandler
    }

    @Test
    void createClient_ConflictMapping_Returns409() throws Exception {
        // Using existing personalId from testClient1
        ClientCreateDTO dto = new ClientCreateDTO(
                testClient1.getPersonalId(), LocalDate.of(1988, 1, 1), "New", "User", "IT",
                "+420", "a@b.c", "444555666", LocalDate.now().minusYears(1),
                LocalDate.now().plusYears(9), "MV CR", getValidAddressInput(), getValidAddressInput()
        );

        mockMvc.perform(post("/api/v1/clients")
                        .with(createTestAdvisorJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value(ErrorCodes.RESOURCE_CONFLICT));
    }

    // --- 7. PUT GENERAL INFO ---

    @Test
    void updateGeneralInfo_HappyPath_Returns200() throws Exception {
        Client c = clientRepository.findById(Objects.requireNonNull(updateClient.getId())).orElseThrow();
        ClientGeneralUpdateDTO dto = new ClientGeneralUpdateDTO(
                c.getVersion(), c.getPersonalId(), c.getBirthDate(), "Updated", "Name",
                "IT", "+420", "a@b.c", getValidAddressInput(), getValidAddressInput()
        );

        mockMvc.perform(put("/api/v1/clients/" + updateClient.getClientUid() + "/general-info")
                        .with(advisor1Jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Updated"));
    }

    @Test
    void updateGeneralInfo_OptimisticLockingConflict_Returns409() throws Exception {
        Client c = clientRepository.findById(Objects.requireNonNull(updateClient.getId())).orElseThrow();
        ClientGeneralUpdateDTO dto = new ClientGeneralUpdateDTO(
                9999, c.getPersonalId(), c.getBirthDate(), "Updated", "Name", // Stale version
                "IT", "+420", "a@b.c", getValidAddressInput(), getValidAddressInput()
        );

        mockMvc.perform(put("/api/v1/clients/" + updateClient.getClientUid() + "/general-info")
                        .with(advisor1Jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value(ErrorCodes.VERSION_MISMATCH));
    }

    @Test
    void updateGeneralInfo_MissingVersion_Returns400() throws Exception {
        String jsonPayload = """
                {
                    "personalId": "1234567890",
                    "birthDate": "1990-01-01",
                    "firstName": "John",
                    "lastName": "Doe",
                    "occupation": "IT",
                    "phone": "+420",
                    "email": "a@b.c",
                    "residentialAddress": { "street": "S", "houseNumber": "1", "city": "C", "postalCode": "111 11" },
                    "contactAddress": { "street": "S", "houseNumber": "1", "city": "C", "postalCode": "111 11" }
                }
                """;

        mockMvc.perform(put("/api/v1/clients/" + updateClient.getClientUid() + "/general-info")
                        .with(advisor1Jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.invalid_params.version").exists());
    }

    // --- 8. PUT ID CARD ---

    @Test
    void updateIdCard_HappyPath_Returns200() throws Exception {
        Client c = clientRepository.findById(Objects.requireNonNull(updateClient.getId())).orElseThrow();
        ClientIdCardUpdateDTO dto = new ClientIdCardUpdateDTO(
                c.getVersion(), "999888777", LocalDate.now().minusYears(2), LocalDate.now().plusYears(8), "New Issuer"
        );

        mockMvc.perform(put("/api/v1/clients/" + updateClient.getClientUid() + "/id-card")
                        .with(advisor1Jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idCardNumber").value("999888777"));
    }

    @Test
    void updateIdCard_ValidationFailure_IdLength_Returns400() throws Exception {
        Client c = clientRepository.findById(Objects.requireNonNull(updateClient.getId())).orElseThrow();
        ClientIdCardUpdateDTO dto = new ClientIdCardUpdateDTO(
                c.getVersion(), "12345", LocalDate.now().minusYears(2), LocalDate.now().plusYears(8), "New Issuer" // Only 5 digits
        );

        mockMvc.perform(put("/api/v1/clients/" + updateClient.getClientUid() + "/id-card")
                        .with(advisor1Jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.invalid_params.idCardNumber").exists());
    }

    @Test
    void updateIdCard_BusinessRuleMapping_Returns400() throws Exception {
        Client c = clientRepository.findById(Objects.requireNonNull(updateClient.getId())).orElseThrow();
        ClientIdCardUpdateDTO dto = new ClientIdCardUpdateDTO(
                c.getVersion(), "999888777", LocalDate.now().plusYears(2), LocalDate.now().minusYears(2), "New Issuer" // Issue after expiry
        );

        mockMvc.perform(put("/api/v1/clients/" + updateClient.getClientUid() + "/id-card")
                        .with(advisor1Jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(ErrorCodes.INVALID_INPUT_VALUE));
    }

    // --- 9. PATCH STATUS ---

    @Test
    void updateStatus_HappyPath_Admin_Returns200() throws Exception {
        ClientStatusUpdateDTO dto = new ClientStatusUpdateDTO(false);

        mockMvc.perform(patch("/api/v1/clients/" + updateClient.getClientUid() + "/status")
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));
    }

    @Test
    void updateStatus_SecurityEnforcement_Advisor_Returns403() throws Exception {
        ClientStatusUpdateDTO dto = new ClientStatusUpdateDTO(false);

        mockMvc.perform(patch("/api/v1/clients/" + updateClient.getClientUid() + "/status")
                        .with(advisor1Jwt()) // Advisor trying to hit admin endpoint
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateStatus_ValidationFailure_Returns400() throws Exception {
        mockMvc.perform(patch("/api/v1/clients/" + updateClient.getClientUid() + "/status")
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.invalid_params.isActive").exists());
    }
}