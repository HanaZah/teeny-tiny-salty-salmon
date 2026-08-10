package com.finadvise.crm.budget;

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

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClientBudgetControllerIT extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private EntityManager entityManager;
    @Autowired private TransactionTemplate transactionTemplate;

    @Autowired private IncomeTypeRepository incomeTypeRepository;
    @Autowired private ExpenseTypeRepository expenseTypeRepository;
    @Autowired private IncomeRepository incomeRepository;
    @Autowired private ExpenseRepository expenseRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private User testAdmin;
    private User testAdvisor1;
    private User testAdvisor2;
    private Client readClient;
    private Client updateClient;
    private Client crossAdvisorClient;

    private IncomeType typeSalary;
    private ExpenseType typeRent;

    @BeforeAll
    void setUpAll() {
        transactionTemplate.executeWithoutResult(status -> {
            // 1. Wipe database cleanly
            cleanDatabase();

            String hash = passwordEncoder.encode("secret");

            // 2. Setup Address
            Address testAddress = TestFixtureFactory.createIntegrationAddress(1001);
            entityManager.persist(testAddress);

            // 3. Setup Users
            testAdmin = TestFixtureFactory.createIntegrationAdmin(1001L, "IT-ADM-1001", hash);
            entityManager.persist(testAdmin);

            testAdvisor1 = TestFixtureFactory.createIntegrationUser(1002L, "IT-ADV-1001", hash, UserType.ADVISOR);
            entityManager.persist(testAdvisor1);

            testAdvisor2 = TestFixtureFactory.createIntegrationUser(1003L, "IT-ADV-1002", hash, UserType.ADVISOR);
            entityManager.persist(testAdvisor2);

            // 4. Setup Clients
            readClient = TestFixtureFactory.createIntegrationClient(1001L, "UID-CBUDGET-R1", testAdvisor1, testAddress);
            entityManager.persist(readClient);

            updateClient = TestFixtureFactory.createIntegrationClient(1002L, "UID-CBUDGET-U1", testAdvisor1, testAddress);
            entityManager.persist(updateClient);

            crossAdvisorClient = TestFixtureFactory.createIntegrationClient(1003L, "UID-CBUDGET-X1", testAdvisor2, testAddress);
            entityManager.persist(crossAdvisorClient);

            entityManager.flush();

            // 5. Setup Budget Types
            typeSalary = incomeTypeRepository.save(IncomeType.builder().name("CTRL_SALARY_TYPE").build());
            typeRent = expenseTypeRepository.save(ExpenseType.builder().name("CTRL_RENT_TYPE").build());

            // 6. Seed Initial Budget Items for the updateClient
            incomeRepository.save(TestFixtureFactory.createIntegrationIncome(updateClient, typeSalary, 5000));
            expenseRepository.save(TestFixtureFactory.createIntegrationExpense(updateClient, typeRent, 1500, true));
        });
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor adminJwt() {
        return jwt().jwt(j -> j.subject(testAdmin.getEmployeeId())).authorities(new SimpleGrantedAuthority("ADMIN"));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor advisorJwt() {
        return jwt().jwt(j -> j.subject(testAdvisor1.getEmployeeId())).authorities(new SimpleGrantedAuthority("ADVISOR"));
    }

    // --- GLOBAL SECURITY CONSTRAINTS ---

    @Test
    void globalSecurity_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(put("/api/v1/clients/" + readClient.getClientUid() + "/budget")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void globalSecurity_AdminCallingUpdate_Returns403() throws Exception {
        FullBudgetUpdateDTO request = new FullBudgetUpdateDTO(List.of(), List.of());

        mockMvc.perform(put("/api/v1/clients/" + readClient.getClientUid() + "/budget")
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // --- UPDATE CLIENT BUDGET ---

    @Test
    void updateClientBudget_Success_Returns200() throws Exception {
        FullBudgetUpdateDTO request = new FullBudgetUpdateDTO(
                List.of(new IncomeUpdateDTO(6000, typeSalary.getId())),
                List.of(new ExpenseUpdateDTO(1600, typeRent.getId(), true))
        );

        mockMvc.perform(put("/api/v1/clients/" + updateClient.getClientUid() + "/budget")
                        .with(advisorJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incomes[0].amount").value(6000))
                .andExpect(jsonPath("$.expenses[0].amount").value(1600))
                .andExpect(jsonPath("$.totalCashFlow").value(4400));
    }

    @Test
    void updateClientBudget_InvalidPayload_Returns400() throws Exception {
        FullBudgetUpdateDTO request = new FullBudgetUpdateDTO(
                List.of(new IncomeUpdateDTO(-500, typeSalary.getId())), // Invalid negative amount
                List.of()
        );

        mockMvc.perform(put("/api/v1/clients/" + readClient.getClientUid() + "/budget")
                        .with(advisorJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(ErrorCodes.VALIDATION_FAILED))
                .andExpect(jsonPath("$.invalid_params['incomes[0].amount']").exists());
    }

    @Test
    void updateClientBudget_DuplicateTypes_Returns400() throws Exception {
        FullBudgetUpdateDTO request = new FullBudgetUpdateDTO(
                List.of(
                        new IncomeUpdateDTO(1000, typeSalary.getId()),
                        new IncomeUpdateDTO(2000, typeSalary.getId()) // Duplicate Type
                ),
                List.of()
        );

        mockMvc.perform(put("/api/v1/clients/" + readClient.getClientUid() + "/budget")
                        .with(advisorJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(ErrorCodes.INVALID_INPUT_VALUE));
    }

    @Test
    void updateClientBudget_InvalidTypeId_Returns400() throws Exception {
        FullBudgetUpdateDTO request = new FullBudgetUpdateDTO(
                List.of(new IncomeUpdateDTO(1000, 99999L)), // Non-existent Type
                List.of()
        );

        mockMvc.perform(put("/api/v1/clients/" + readClient.getClientUid() + "/budget")
                        .with(advisorJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(ErrorCodes.INVALID_INPUT_VALUE));
    }

    @Test
    void updateClientBudget_WrongAdvisorOrNotFound_Returns404() throws Exception {
        FullBudgetUpdateDTO request = new FullBudgetUpdateDTO(List.of(), List.of());

        mockMvc.perform(put("/api/v1/clients/" + crossAdvisorClient.getClientUid() + "/budget")
                        .with(advisorJwt()) // Authenticated as Advisor 1, but Client belongs to Advisor 2
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value(ErrorCodes.RESOURCE_NOT_FOUND));
    }
}