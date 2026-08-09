package com.finadvise.crm.products;

import com.finadvise.crm.AbstractIntegrationTest;
import com.finadvise.crm.TestFixtureFactory;
import com.finadvise.crm.addresses.Address;
import com.finadvise.crm.clients.Client;
import com.finadvise.crm.common.ErrorCodes;
import com.finadvise.crm.users.User;
import com.finadvise.crm.users.UserType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClientProductControllerIT extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private EntityManager entityManager;
    @Autowired private TransactionTemplate transactionTemplate;

    @Autowired private ProviderRepository providerRepository;
    @Autowired private ProductTypeRepository productTypeRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private User testAdmin;
    private User testAdvisor;
    private Client testClient;
    private ProductType dbType;
    private Provider dbProvider;

    @BeforeAll
    void setUpAll() {
        transactionTemplate.executeWithoutResult(status -> {
            cleanDatabase();

            String hash = passwordEncoder.encode("secret");

            Address testAddress = TestFixtureFactory.createIntegrationAddress(801);
            entityManager.persist(testAddress);

            testAdmin = TestFixtureFactory.createIntegrationAdmin(801L, "IT-ADM-801", hash);
            entityManager.persist(testAdmin);

            testAdvisor = TestFixtureFactory.createIntegrationUser(802L, "IT-ADV-801", hash, UserType.ADVISOR);
            entityManager.persist(testAdvisor);

            testClient = TestFixtureFactory.createIntegrationClient(801L, "UID-CPROD-C1", testAdvisor, testAddress);
            entityManager.persist(testClient);

            entityManager.flush();

            dbType = productTypeRepository.save(ProductType.builder().name("ClientProduct Type").build());
            dbProvider = providerRepository.save(Provider.builder().name("ClientProduct Provider").build());
        });
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
        mockMvc.perform(post("/api/v1/clients/" + testClient.getClientUid() + "/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void globalSecurity_AdminCallingCreate_Returns403() throws Exception {
        ProductCreateDTO validPayloadToBypassValidation = new ProductCreateDTO(
                "Dummy Product", new BigDecimal("500.00"), LocalDate.now(), null, dbType.getId(), dbProvider.getId(), false
        );

        mockMvc.perform(post("/api/v1/clients/" + testClient.getClientUid() + "/products")
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPayloadToBypassValidation)))
                .andExpect(status().isForbidden());
    }

    // --- CREATE PRODUCT ---

    @Test
    void createProduct_Success_Returns201() throws Exception {
        ProductCreateDTO request = new ProductCreateDTO(
                "New Controller Product", new BigDecimal("500.00"), LocalDate.now(), null, dbType.getId(), dbProvider.getId(), false
        );

        mockMvc.perform(post("/api/v1/clients/" + testClient.getClientUid() + "/products")
                        .with(advisorJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("New Controller Product"))
                .andExpect(jsonPath("$.advisor.employeeId").value(testAdvisor.getEmployeeId()));
    }

    @Test
    void createProduct_ValidationFailure_Returns400() throws Exception {
        ProductCreateDTO request = new ProductCreateDTO(
                "", new BigDecimal("500.00"), LocalDate.now(), null, dbType.getId(), dbProvider.getId(), false
        );

        mockMvc.perform(post("/api/v1/clients/" + testClient.getClientUid() + "/products")
                        .with(advisorJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(ErrorCodes.VALIDATION_FAILED))
                .andExpect(jsonPath("$.invalid_params.name").exists());
    }

    @Test
    void createProduct_NotFound_Returns404() throws Exception {
        ProductCreateDTO request = new ProductCreateDTO(
                "New Controller Product", new BigDecimal("500.00"), LocalDate.now(), null, dbType.getId(), dbProvider.getId(), false
        );

        mockMvc.perform(post("/api/v1/clients/UNKNOWN/products")
                        .with(advisorJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value(ErrorCodes.RESOURCE_NOT_FOUND));
    }
}