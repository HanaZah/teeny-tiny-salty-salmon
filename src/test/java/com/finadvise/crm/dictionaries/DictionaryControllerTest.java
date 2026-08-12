package com.finadvise.crm.dictionaries;

import com.finadvise.crm.AbstractIntegrationTest;
import com.finadvise.crm.budget.IncomeType;
import com.finadvise.crm.common.ErrorCodes;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DictionaryControllerIT extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private TransactionTemplate transactionTemplate;

    @BeforeAll
    void setUpAll() {
        transactionTemplate.executeWithoutResult(status -> {
            cleanDatabase();

            // Seed a highly distinctive dummy value to verify database retrieval
            entityManager.persist(IncomeType.builder().name("CTRL_DUMMY_INCOME_TYPE").build());

            entityManager.flush();
        });
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor authenticatedUser() {
        // Any authenticated user can access dictionaries, so a generic JWT is sufficient
        return jwt().jwt(j -> j.subject("GENERIC_USER")).authorities(new SimpleGrantedAuthority("ADVISOR"));
    }

    // --- 1. GLOBAL SECURITY ---

    @Test
    void globalSecurity_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/dictionaries/dynamic/income-type")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    // --- 2. DYNAMIC DICTIONARIES ---

    @Test
    void getDynamicDictionaryItems_HappyPath_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/dictionaries/dynamic/income-type")
                        .with(authenticatedUser())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].label").value("CTRL_DUMMY_INCOME_TYPE"));
    }

    @Test
    void getDynamicDictionaryItems_InvalidType_Returns400() throws Exception {
        mockMvc.perform(get("/api/v1/dictionaries/dynamic/invalid-type")
                        .with(authenticatedUser())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(ErrorCodes.VALIDATION_FAILED))
                .andExpect(jsonPath("$.invalid_params.type").exists());
    }

    // --- 3. STATIC DICTIONARIES ---

    @Test
    void getStaticDictionaryItems_HappyPath_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/dictionaries/static/client-status")
                        .with(authenticatedUser())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.value == 'ACTIVE')]").exists())
                .andExpect(jsonPath("$[?(@.value == 'INACTIVE')]").exists());
    }

    @Test
    void getStaticDictionaryItems_IndexEndpoint_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/dictionaries/static/dynamic-dictionaries")
                        .with(authenticatedUser())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                // Verifying that our path values map correctly to the index
                .andExpect(jsonPath("$[?(@.value == 'income-type')]").exists())
                .andExpect(jsonPath("$[?(@.value == 'product-type')]").exists());
    }

    @Test
    void getStaticDictionaryItems_InvalidType_Returns400() throws Exception {
        mockMvc.perform(get("/api/v1/dictionaries/static/fake-status")
                        .with(authenticatedUser())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(ErrorCodes.VALIDATION_FAILED))
                .andExpect(jsonPath("$.invalid_params.type").exists());
    }
}