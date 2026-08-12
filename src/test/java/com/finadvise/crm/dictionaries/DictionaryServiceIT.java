package com.finadvise.crm.dictionaries;

import com.finadvise.crm.AbstractIntegrationTest;
import com.finadvise.crm.budget.ExpenseType;
import com.finadvise.crm.budget.IncomeType;
import com.finadvise.crm.products.ProductType;
import com.finadvise.crm.products.Provider;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DictionaryServiceIT extends AbstractIntegrationTest {

    @Autowired
    private DictionaryService dictionaryService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @BeforeAll
    void setUpAll() {
        transactionTemplate.executeWithoutResult(status -> {
            // Wipe database to ensure idempotent runs
            cleanDatabase();

            // Seed Income Types (Inserted in reverse alphabetical order to test sorting)
            entityManager.persist(IncomeType.builder().name("ZZZ_DUMMY_INCOME").build());
            entityManager.persist(IncomeType.builder().name("AAA_DUMMY_INCOME").build());

            // Seed Expense Types
            entityManager.persist(ExpenseType.builder().name("ZZZ_DUMMY_EXPENSE").build());
            entityManager.persist(ExpenseType.builder().name("AAA_DUMMY_EXPENSE").build());

            // Seed Product Types
            entityManager.persist(ProductType.builder().name("ZZZ_DUMMY_PRODTYPE").build());
            entityManager.persist(ProductType.builder().name("AAA_DUMMY_PRODTYPE").build());

            // Seed Providers
            entityManager.persist(Provider.builder().name("ZZZ_DUMMY_PROVIDER").build());
            entityManager.persist(Provider.builder().name("AAA_DUMMY_PROVIDER").build());

            entityManager.flush();
        });
    }

    // --- 1. DYNAMIC DICTIONARIES (DATABASE PERSISTENCE & SORTING) ---

    @Test
    @WithMockUser
    void getDynamicDictionaryItems_IncomeTypes_ReturnsSortedList() {
        List<DynamicDictionaryItemDTO> results = dictionaryService.getDynamicDictionaryItems(DynamicDictionaryType.INCOME_TYPE);

        List<DynamicDictionaryItemDTO> dummyResults = results.stream()
                .filter(item -> item.label().contains("DUMMY"))
                .toList();

        assertEquals(2, dummyResults.size());
        assertEquals("AAA_DUMMY_INCOME", dummyResults.get(0).label());
        assertEquals("ZZZ_DUMMY_INCOME", dummyResults.get(1).label());
    }

    @Test
    @WithMockUser
    void getDynamicDictionaryItems_ExpenseTypes_ReturnsSortedList() {
        List<DynamicDictionaryItemDTO> results = dictionaryService.getDynamicDictionaryItems(DynamicDictionaryType.EXPENSE_TYPE);

        List<DynamicDictionaryItemDTO> dummyResults = results.stream()
                .filter(item -> item.label().contains("DUMMY"))
                .toList();

        assertEquals(2, dummyResults.size());
        assertEquals("AAA_DUMMY_EXPENSE", dummyResults.get(0).label());
        assertEquals("ZZZ_DUMMY_EXPENSE", dummyResults.get(1).label());
    }

    @Test
    @WithMockUser
    void getDynamicDictionaryItems_ProductTypes_ReturnsSortedList() {
        List<DynamicDictionaryItemDTO> results = dictionaryService.getDynamicDictionaryItems(DynamicDictionaryType.PRODUCT_TYPE);

        List<DynamicDictionaryItemDTO> dummyResults = results.stream()
                .filter(item -> item.label().contains("DUMMY"))
                .toList();

        assertEquals(2, dummyResults.size());
        assertEquals("AAA_DUMMY_PRODTYPE", dummyResults.get(0).label());
        assertEquals("ZZZ_DUMMY_PRODTYPE", dummyResults.get(1).label());
    }

    @Test
    @WithMockUser
    void getDynamicDictionaryItems_Providers_ReturnsSortedList() {
        List<DynamicDictionaryItemDTO> results = dictionaryService.getDynamicDictionaryItems(DynamicDictionaryType.PRODUCT_PROVIDER);

        List<DynamicDictionaryItemDTO> dummyResults = results.stream()
                .filter(item -> item.label().contains("DUMMY"))
                .toList();

        assertEquals(2, dummyResults.size());
        assertEquals("AAA_DUMMY_PROVIDER", dummyResults.get(0).label());
        assertEquals("ZZZ_DUMMY_PROVIDER", dummyResults.get(1).label());
    }

    // --- 2. STATIC DICTIONARIES (CONTEXT WIRING & ENUM MAPPING) ---

    @Test
    @WithMockUser
    void getStaticDictionaryItems_EnumBasedDictionaries_ReturnsMappedValues() {
        // Test Client Status
        List<StaticDictionaryItemDTO> clientStatuses = dictionaryService.getStaticDictionaryItems(StaticDictionaryType.CLIENT_STATUS);
        assertFalse(clientStatuses.isEmpty());
        assertTrue(clientStatuses.stream().anyMatch(s -> s.value().equals("ACTIVE")));

        // Test Product Status
        List<StaticDictionaryItemDTO> productStatuses = dictionaryService.getStaticDictionaryItems(StaticDictionaryType.PRODUCT_STATUS);
        assertFalse(productStatuses.isEmpty());
        assertTrue(productStatuses.stream().anyMatch(s -> s.value().equals("ACTIVE")));

        // Test User Status
        List<StaticDictionaryItemDTO> userStatuses = dictionaryService.getStaticDictionaryItems(StaticDictionaryType.USER_STATUS);
        assertFalse(userStatuses.isEmpty());
        assertTrue(userStatuses.stream().anyMatch(s -> s.value().equals("ACTIVE")));

        // Test User Types
        List<StaticDictionaryItemDTO> userTypes = dictionaryService.getStaticDictionaryItems(StaticDictionaryType.USER_TYPE);
        assertFalse(userTypes.isEmpty());
        assertTrue(userTypes.stream().anyMatch(s -> s.value().equals("ADVISOR")));
    }

    @Test
    @WithMockUser
    void getStaticDictionaryItems_DynamicDictionariesIndex_ReturnsMappedValues() {
        List<StaticDictionaryItemDTO> results = dictionaryService.getStaticDictionaryItems(StaticDictionaryType.DYNAMIC_DICTIONARIES);

        assertNotNull(results);
        assertEquals(DynamicDictionaryType.values().length, results.size());

        // Verify a specific path mapping was translated correctly
        assertTrue(results.stream().anyMatch(s -> s.value().equals("income-type")));
    }

    // --- 3. GLOBAL SECURITY CONSTRAINTS ---

    @Test
    void globalSecurity_UnauthenticatedAccess_ThrowsException() {
        // Executing without @WithMockUser annotation
        assertThrows(AuthenticationCredentialsNotFoundException.class, () ->
                dictionaryService.getDynamicDictionaryItems(DynamicDictionaryType.INCOME_TYPE)
        );
    }
}