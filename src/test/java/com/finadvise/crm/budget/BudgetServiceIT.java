package com.finadvise.crm.budget;

import com.finadvise.crm.AbstractIntegrationTest;
import com.finadvise.crm.TestFixtureFactory;
import com.finadvise.crm.addresses.Address;
import com.finadvise.crm.clients.Client;
import com.finadvise.crm.common.InvalidInputValueException;
import com.finadvise.crm.common.ResourceNotFoundException;
import com.finadvise.crm.users.User;
import com.finadvise.crm.users.UserType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BudgetServiceIT extends AbstractIntegrationTest {

    @Autowired private BudgetService budgetService;

    // Package-private repositories can be safely autowired here because we are in the 'budget' package
    @Autowired private IncomeRepository incomeRepository;
    @Autowired private ExpenseRepository expenseRepository;
    @Autowired private IncomeTypeRepository incomeTypeRepository;
    @Autowired private ExpenseTypeRepository expenseTypeRepository;

    @Autowired private EntityManager entityManager;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private PasswordEncoder passwordEncoder;

    private User testAdmin;
    private User testAdvisor1;
    private User testAdvisor2;
    private Client testClient1;
    private Client testClient2;
    private Client emptyClient;
    private Client updateClient;

    private IncomeType typeSalary;
    private IncomeType typeBonus;
    private ExpenseType typeRent;
    private ExpenseType typeGroceries;

    @BeforeAll
    void setUpAll() {
        transactionTemplate.executeWithoutResult(status -> {

            // 1. Wipe everything cleanly in strict FK order
            cleanDatabase();

            String hash = passwordEncoder.encode("secret");

            // 2. Setup Address (Cross-package via EntityManager)
            Address testAddress = TestFixtureFactory.createIntegrationAddress(901);
            entityManager.persist(testAddress);

            // 3. Setup Users (Cross-package via EntityManager)
            testAdmin = TestFixtureFactory.createIntegrationAdmin(901L, "IT-BUDGET-ADM-1", hash);
            entityManager.persist(testAdmin);

            testAdvisor1 = TestFixtureFactory.createIntegrationUser(902L, "IT-BUDGET-ADV-1", hash, UserType.ADVISOR);
            entityManager.persist(testAdvisor1);

            testAdvisor2 = TestFixtureFactory.createIntegrationUser(903L, "IT-BUDGET-ADV-2", hash, UserType.ADVISOR);
            entityManager.persist(testAdvisor2);

            // 4. Setup Clients (Cross-package via EntityManager)
            testClient1 = TestFixtureFactory.createIntegrationClient(901L, "UID-BUDGET-001", testAdvisor1, testAddress);
            entityManager.persist(testClient1);

            testClient2 = TestFixtureFactory.createIntegrationClient(902L, "UID-BUDGET-002", testAdvisor2, testAddress);
            entityManager.persist(testClient2);

            emptyClient = TestFixtureFactory.createIntegrationClient(903L, "UID-BUDGET-003", testAdvisor1, testAddress);
            entityManager.persist(emptyClient);

            updateClient = TestFixtureFactory.createIntegrationClient(904L, "UID-BUDGET-UPD", testAdvisor1, testAddress);
            entityManager.persist(updateClient);

            // Flush clients so budget items can reference them safely
            entityManager.flush();

            // 5. Setup Budget Types (Highly distinct names to avoid DB Seeder collisions)
            typeSalary = incomeTypeRepository.save(IncomeType.builder().name("TEST_DUMMY_INCOME_SALARY_999").build());
            typeBonus = incomeTypeRepository.save(IncomeType.builder().name("TEST_DUMMY_INCOME_BONUS_999").build());
            typeRent = expenseTypeRepository.save(ExpenseType.builder().name("TEST_DUMMY_EXPENSE_RENT_999").build());
            typeGroceries = expenseTypeRepository.save(ExpenseType.builder().name("TEST_DUMMY_EXPENSE_GROC_999").build());

            // 6. Seed Initial Budget Items for testClient1 and updateClient
            incomeRepository.save(TestFixtureFactory.createIntegrationIncome(testClient1, typeSalary, 5000));
            expenseRepository.save(TestFixtureFactory.createIntegrationExpense(testClient1, typeRent, 1500, true));
            expenseRepository.save(TestFixtureFactory.createIntegrationExpense(testClient1, typeGroceries, 500, false));

            incomeRepository.save(TestFixtureFactory.createIntegrationIncome(updateClient, typeSalary, 5000));
            expenseRepository.save(TestFixtureFactory.createIntegrationExpense(updateClient, typeRent, 1500, true));
            expenseRepository.save(TestFixtureFactory.createIntegrationExpense(updateClient, typeGroceries, 500, false));
        });
    }

    // --- GET FULL BUDGET ---

    @Test
    void getFullBudgetForClient_PopulatedBudget_CalculatesCorrectly() {
        FullBudgetDTO result = budgetService.getFullBudgetForClient(testClient1.getClientUid());

        assertNotNull(result);
        assertEquals(1, result.incomes().size());
        assertEquals(2, result.expenses().size());
        // 5000 (Salary) - 1500 (Rent) - 500 (Groceries) = 3000
        assertEquals(3000, result.totalCashFlow());
    }

    @Test
    void getFullBudgetForClient_EmptyBudget_ReturnsZeroCashFlow() {
        FullBudgetDTO result = budgetService.getFullBudgetForClient(emptyClient.getClientUid());

        assertNotNull(result);
        assertTrue(result.incomes().isEmpty());
        assertTrue(result.expenses().isEmpty());
        assertEquals(0, result.totalCashFlow());
    }

    // --- UPDATE FULL BUDGET ---

    @Test
    @WithMockUser(username = "IT-BUDGET-ADV-1", authorities = "ADVISOR")
    void updateFullBudgetForClient_DatabasePersistence_FullCycle() {
        // We will update Salary, add Bonus, update Rent (change amount/mandatory), and delete Groceries
        FullBudgetUpdateDTO request = new FullBudgetUpdateDTO(
                List.of(
                        new IncomeUpdateDTO(6000, typeSalary.getId()), // Update
                        new IncomeUpdateDTO(1000, typeBonus.getId())   // Create
                ),
                List.of(
                        new ExpenseUpdateDTO(1600, typeRent.getId(), false), // Update Amount & Mandatory Flag
                        new ExpenseUpdateDTO(0, typeGroceries.getId(), false) // Delete
                )
        );

        FullBudgetDTO result = budgetService.updateFullBudgetForClient(updateClient.getClientUid(), testAdvisor1.getEmployeeId(), request);

        // Verify return DTO accuracy
        assertEquals(2, result.incomes().size());
        assertEquals(1, result.expenses().size());
        // (6000 + 1000) - (1600) = 5400
        assertEquals(5400, result.totalCashFlow());

        // Verify Database state
        List<Income> dbIncomes = incomeRepository.findAllByClient_ClientUid(updateClient.getClientUid());
        assertEquals(2, dbIncomes.size());

        List<Expense> dbExpenses = expenseRepository.findAllByClient_ClientUid(updateClient.getClientUid());
        assertEquals(1, dbExpenses.size());
        assertEquals(1600, dbExpenses.getFirst().getAmount());
        assertFalse(dbExpenses.getFirst().isMandatory());
    }

    @Test
    @WithMockUser(username = "IT-BUDGET-ADV-1", authorities = "ADVISOR")
    void updateFullBudgetForClient_UniqueTypeConstraintViolation_RejectsUpdate() {
        FullBudgetUpdateDTO request = new FullBudgetUpdateDTO(
                List.of(
                        new IncomeUpdateDTO(1000, typeSalary.getId()),
                        new IncomeUpdateDTO(500, typeSalary.getId()) // Duplicate Type!
                ),
                List.of()
        );

        assertThrows(InvalidInputValueException.class, () ->
                budgetService.updateFullBudgetForClient(updateClient.getClientUid(), testAdvisor1.getEmployeeId(), request)
        );
    }

    @Test
    @WithMockUser(username = "IT-BUDGET-ADV-1", authorities = "ADVISOR")
    void updateFullBudgetForClient_OwnershipEnforcement_ThrowsNotFound() {
        FullBudgetUpdateDTO request = new FullBudgetUpdateDTO(List.of(), List.of());

        // Advisor 1 attempting to update Client 2 (owned by Advisor 2)
        assertThrows(ResourceNotFoundException.class, () ->
                budgetService.updateFullBudgetForClient(testClient2.getClientUid(), testAdvisor1.getEmployeeId(), request)
        );
    }

    @Test
    @WithMockUser(username = "IT-BUDGET-ADV-1", authorities = "ADVISOR")
    void updateFullBudgetForClient_ForeignKeyConstraintViolation_ThrowsInvalidInputValue() {
        FullBudgetUpdateDTO request = new FullBudgetUpdateDTO(
                List.of(new IncomeUpdateDTO(5000, 99999L)), // 99999L does not exist
                List.of()
        );

        assertThrows(InvalidInputValueException.class, () ->
                budgetService.updateFullBudgetForClient(updateClient.getClientUid(), testAdvisor1.getEmployeeId(), request)
        );
    }

    @Test
    @WithMockUser(username = "IT-ADM-1", authorities = "ADMIN")
    void updateFullBudgetForClient_GlobalSecurityConstraint_AdminDenied() {
        FullBudgetUpdateDTO request = new FullBudgetUpdateDTO(List.of(), List.of());

        assertThrows(AccessDeniedException.class, () ->
                budgetService.updateFullBudgetForClient(updateClient.getClientUid(), testAdmin.getEmployeeId(), request)
        );
    }
}