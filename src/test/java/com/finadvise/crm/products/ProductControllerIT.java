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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProductControllerIT extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private EntityManager entityManager;
    @Autowired private TransactionTemplate transactionTemplate;

    @Autowired private ProductRepository productRepository;
    @Autowired private ProviderRepository providerRepository;
    @Autowired private ProductTypeRepository productTypeRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private User testAdmin;
    private User testAdvisor;
    private Client testClient;
    private ProductType dbType;
    private Provider dbProvider;
    private Product testProduct;

    @BeforeAll
    void setUpAll() {
        transactionTemplate.executeWithoutResult(status -> {
            cleanDatabase();

            String hash = passwordEncoder.encode("secret");

            Address testAddress = TestFixtureFactory.createIntegrationAddress(701);
            entityManager.persist(testAddress);

            testAdmin = TestFixtureFactory.createIntegrationAdmin(701L, "IT-ADM-701", hash);
            entityManager.persist(testAdmin);

            testAdvisor = TestFixtureFactory.createIntegrationUser(702L, "IT-ADV-701", hash, UserType.ADVISOR);
            entityManager.persist(testAdvisor);

            testClient = TestFixtureFactory.createIntegrationClient(701L, "UID-PROD-C1", testAdvisor, testAddress);
            entityManager.persist(testClient);

            entityManager.flush();

            dbType = productTypeRepository.save(ProductType.builder().name("Controller Type").build());
            dbProvider = providerRepository.save(Provider.builder().name("Controller Provider").build());

            testProduct = productRepository.save(Product.builder()
                    .name("Controller Product")
                    .amount(new BigDecimal("1000.00"))
                    .startDate(LocalDate.now().minusMonths(1))
                    .endDate(null)
                    .productType(dbType)
                    .provider(dbProvider)
                    .client(testClient)
                    .advisor(testAdvisor)
                    .build());
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
        mockMvc.perform(put("/api/v1/products/" + testProduct.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void globalSecurity_AdminCallingUpdate_Returns403() throws Exception {
        ProductUpdateDTO validRequest = new ProductUpdateDTO(
                "Dummy Controller Product", new BigDecimal("1500.00"), LocalDate.now(), null, dbType.getId(), dbProvider.getId()
        );
        mockMvc.perform(put("/api/v1/products/" + testProduct.getId())
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden());
    }

    // --- UPDATE PRODUCT ---

    @Test
    void updateProduct_Success_Returns200() throws Exception {
        ProductUpdateDTO request = new ProductUpdateDTO(
                "Updated Controller Product", new BigDecimal("1500.00"), LocalDate.now(), null, dbType.getId(), dbProvider.getId()
        );

        mockMvc.perform(put("/api/v1/products/" + testProduct.getId())
                        .with(advisorJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Controller Product"))
                .andExpect(jsonPath("$.amount").value(1500.00));
    }

    @Test
    void updateProduct_ValidationFailure_Returns400() throws Exception {
        ProductUpdateDTO request = new ProductUpdateDTO(
                "Product", new BigDecimal("-100.00"), LocalDate.now(), null, dbType.getId(), dbProvider.getId()
        );

        mockMvc.perform(put("/api/v1/products/" + testProduct.getId())
                        .with(advisorJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(ErrorCodes.VALIDATION_FAILED))
                .andExpect(jsonPath("$.invalid_params.amount").exists());
    }

    @Test
    void updateProduct_BusinessRuleViolation_Returns400() throws Exception {
        ProductUpdateDTO request = new ProductUpdateDTO(
                "Product", new BigDecimal("100.00"), LocalDate.now(), LocalDate.now().minusDays(1), dbType.getId(), dbProvider.getId()
        );

        mockMvc.perform(put("/api/v1/products/" + testProduct.getId())
                        .with(advisorJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(ErrorCodes.INVALID_INPUT_VALUE));
    }

    @Test
    void updateProduct_NotFound_Returns404() throws Exception {
        ProductUpdateDTO request = new ProductUpdateDTO(
                "Product", new BigDecimal("100.00"), LocalDate.now(), null, dbType.getId(), dbProvider.getId()
        );

        mockMvc.perform(put("/api/v1/products/999999")
                        .with(advisorJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value(ErrorCodes.RESOURCE_NOT_FOUND));
    }

    // --- SEARCH PRODUCTS ---

    @Test
    void searchProducts_Admin_Returns200() throws Exception {
        ProductSearchCriteriaDTO request = new ProductSearchCriteriaDTO(
                null, testClient.getClientUid(), null, null, null, null
        );

        mockMvc.perform(post("/api/v1/products/search")
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].name").exists());
    }

    @Test
    void searchProducts_Advisor_Returns200() throws Exception {
        ProductSearchCriteriaDTO request = new ProductSearchCriteriaDTO(
                null, testClient.getClientUid(), null, null, null, null
        );

        mockMvc.perform(post("/api/v1/products/search")
                        .with(advisorJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].name").exists());
    }

    @Test
    void searchProducts_ValidationFailure_Returns400() throws Exception {
        ProductSearchCriteriaDTO request = new ProductSearchCriteriaDTO(
                null, "INVALID_UID_@", null, null, null, null
        );

        mockMvc.perform(post("/api/v1/products/search")
                        .with(advisorJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(ErrorCodes.VALIDATION_FAILED))
                .andExpect(jsonPath("$.invalid_params.clientUid").exists());
    }
}