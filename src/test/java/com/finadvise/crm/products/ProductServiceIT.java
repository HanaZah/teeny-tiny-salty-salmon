package com.finadvise.crm.products;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ProductServiceIT extends AbstractIntegrationTest {
    @MockitoSpyBean private Clock clock;
    @Autowired private ProductService productService;
    @Autowired private ProductRepository productRepository;
    @Autowired private ProductTypeRepository productTypeRepository;
    @Autowired private ProviderRepository providerRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    // Use EntityManager and TransactionTemplate for cross-module seeding
    @Autowired private EntityManager entityManager;
    @Autowired private TransactionTemplate transactionTemplate;

    private User testAdmin;
    private User testAdvisor1;
    private User testAdvisor2;
    private Client testClient1;
    private Client testClient2;
    private ProductType dbType;
    private Provider dbProvider;

    private Product activeProductAdv1;
    private Product expiredProductAdv1;
    private Product crossClientProduct;
    private Product externalProduct;
    private Product productAdv2;

    @BeforeAll
    void setUpAll() {
        transactionTemplate.executeWithoutResult(status -> {
            cleanDatabase();

            String hash = passwordEncoder.encode("secret");

            // 1. Setup Address
            Address testAddress = TestFixtureFactory.createIntegrationAddress(601);
            entityManager.persist(testAddress);

            // 2. Setup Users
            testAdmin = TestFixtureFactory.createIntegrationAdmin(601L, "IT-ADM-1", hash);
            entityManager.persist(testAdmin);

            testAdvisor1 = TestFixtureFactory.createIntegrationUser(602L, "IT-ADV-1", hash, UserType.ADVISOR);
            entityManager.persist(testAdvisor1);

            testAdvisor2 = TestFixtureFactory.createIntegrationUser(603L, "IT-ADV-2", hash, UserType.ADVISOR);
            entityManager.persist(testAdvisor2);

            // 3. Setup Clients (Phase 1)
            testClient1 = TestFixtureFactory.createIntegrationClient(601L, "UID-001", testAdvisor1, testAddress);
            entityManager.persist(testClient1);

            // Assign testClient2 initially to Advisor 1 to create the cross-client product
            testClient2 = TestFixtureFactory.createIntegrationClient(602L, "UID-002", testAdvisor1, testAddress);
            entityManager.persist(testClient2);

            // Flush the entities to the database so the repositories can safely reference them
            entityManager.flush();

            // 4. Setup Lookups
            dbType = productTypeRepository.save(ProductType.builder().name("Integration Type").build());
            dbProvider = providerRepository.save(Provider.builder().name("Integration Provider").build());

            // 5. Setup Products (Phase 1 - Client 2 belongs to Advisor 1)
            activeProductAdv1 = productRepository.save(Product.builder()
                    .name("Active Prod 1")
                    .amount(new BigDecimal("1000.00"))
                    .startDate(LocalDate.now().minusMonths(1))
                    .endDate(null)
                    .productType(dbType)
                    .provider(dbProvider)
                    .client(testClient1)
                    .advisor(testAdvisor1)
                    .build());

            expiredProductAdv1 = productRepository.save(Product.builder()
                    .name("Expired Prod")
                    .amount(new BigDecimal("500.00"))
                    .startDate(LocalDate.now().minusYears(2))
                    .endDate(LocalDate.now().minusDays(1))
                    .productType(dbType)
                    .provider(dbProvider)
                    .client(testClient1)
                    .advisor(testAdvisor1)
                    .build());

            externalProduct = productRepository.save(Product.builder()
                    .name("External Prod")
                    .amount(new BigDecimal("200.00"))
                    .startDate(LocalDate.now().minusDays(10))
                    .endDate(null)
                    .productType(dbType)
                    .provider(dbProvider)
                    .client(testClient1)
                    .advisor(null) // Unmanaged
                    .build());

            crossClientProduct = productRepository.save(Product.builder()
                    .name("Cross Client Prod")
                    .amount(new BigDecimal("999.00"))
                    .startDate(LocalDate.now().minusMonths(1))
                    .endDate(null)
                    .productType(dbType)
                    .provider(dbProvider)
                    .client(testClient2) // Currently owned by Advisor 1
                    .advisor(testAdvisor1)
                    .build());

            // 6. Simulate Client Handover
            testClient2.setAdvisor(testAdvisor2);
            testClient2 = entityManager.merge(testClient2);
            entityManager.flush(); // Flush the handover so the DB trigger knows Client 2 now belongs to Advisor 2

            // 7. Setup Products (Phase 2 - Client 2 now belongs to Advisor 2)
            productAdv2 = productRepository.save(Product.builder()
                    .name("Adv 2 Prod")
                    .amount(new BigDecimal("3000.00"))
                    .startDate(LocalDate.now().minusMonths(6))
                    .endDate(null)
                    .productType(dbType)
                    .provider(dbProvider)
                    .client(testClient2)
                    .advisor(testAdvisor2)
                    .build());
        });
    }

    // --- 1. GLOBAL SECURITY CONSTRAINTS ---

    @Test
    @WithMockUser(username = "IT-ADM-1", authorities = "ADMIN")
    void globalSecurity_AdminCallingCreate_ThrowsAccessDenied() {
        ProductCreateDTO dto = new ProductCreateDTO(
                "Test", BigDecimal.TEN, LocalDate.now(), null, dbType.getId(), dbProvider.getId(), false
        );

        assertThrows(AccessDeniedException.class, () ->
                productService.createProduct(dto, testAdmin.getEmployeeId(), testClient1.getClientUid())
        );
    }

    @Test
    @WithMockUser(username = "IT-ADV-1", authorities = "ADVISOR")
    void globalSecurity_AdvisorWrongPrincipal_ThrowsAccessDenied() {
        ProductUpdateDTO dto = new ProductUpdateDTO(
                "Test", BigDecimal.TEN, LocalDate.now(), null, dbType.getId(), dbProvider.getId()
        );

        // Principal is IT-ADV-1, but trying to pass IT-ADV-2
        assertThrows(AccessDeniedException.class, () ->
                productService.updateProduct(productAdv2.getId(), dto, "IT-ADV-2")
        );
    }

    // --- 2. CREATE PRODUCT ---

    @Test
    @WithMockUser(username = "IT-ADV-1", authorities = "ADVISOR")
    void createProduct_Internal_PersistsWithAdvisor() {
        ProductCreateDTO dto = new ProductCreateDTO(
                "New Internal", new BigDecimal("150.00"), LocalDate.now(), null, dbType.getId(), dbProvider.getId(), false
        );

        ProductDTO result = productService.createProduct(dto, testAdvisor1.getEmployeeId(), testClient1.getClientUid());

        assertNotNull(result.id());
        Product dbProduct = productRepository.findById(result.id()).orElseThrow();
        assertNotNull(dbProduct.getAdvisor());
        assertEquals(testAdvisor1.getId(), dbProduct.getAdvisor().getId());
    }

    @Test
    @WithMockUser(username = "IT-ADV-1", authorities = "ADVISOR")
    void createProduct_External_PersistsWithNullAdvisor() {
        ProductCreateDTO dto = new ProductCreateDTO(
                "New External", new BigDecimal("250.00"), LocalDate.now(), null, dbType.getId(), dbProvider.getId(), true
        );

        ProductDTO result = productService.createProduct(dto, testAdvisor1.getEmployeeId(), testClient1.getClientUid());

        assertNotNull(result.id());
        Product dbProduct = productRepository.findById(result.id()).orElseThrow();
        assertNull(dbProduct.getAdvisor());
    }

    @Test
    @WithMockUser(username = "IT-ADV-1", authorities = "ADVISOR")
    void createProduct_ClientIneligible_ThrowsInvalidInputValue() {
        // Dynamically fast-forward time by 2 years to guarantee the ID card expiry date (now + 1 year in TestFixtureFactory) is in the past
        Instant futureInstant = LocalDate.now().plusYears(2)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant();

        doReturn(futureInstant).when(clock).instant();
        doReturn(ZoneId.of("UTC")).when(clock).getZone();

        // Must use the mocked clock for the DTO to pass the standard validation as well
        ProductCreateDTO dto = new ProductCreateDTO(
                "Fail Prod", BigDecimal.TEN, LocalDate.now(clock), null, dbType.getId(), dbProvider.getId(), false
        );

        assertThrows(InvalidInputValueException.class, () ->
                productService.createProduct(dto, testAdvisor1.getEmployeeId(), testClient1.getClientUid())
        );

        // Reset the clock
        org.mockito.Mockito.reset(clock);
    }

    // --- 3. UPDATE PRODUCT ---

    @Test
    @WithMockUser(username = "IT-ADV-1", authorities = "ADVISOR")
    void updateProduct_ValidPayload_PersistsChanges() {
        ProductUpdateDTO dto = new ProductUpdateDTO(
                "Updated Name", new BigDecimal("1234.56"), LocalDate.now(), null, dbType.getId(), dbProvider.getId()
        );

        ProductDTO result = productService.updateProduct(activeProductAdv1.getId(), dto, testAdvisor1.getEmployeeId());

        assertEquals("Updated Name", result.name());
        assertEquals(new BigDecimal("1234.56"), result.amount());

        Product dbProduct = productRepository.findById(activeProductAdv1.getId()).orElseThrow();
        assertEquals("Updated Name", dbProduct.getName());
    }

    @Test
    @WithMockUser(username = "IT-ADV-1", authorities = "ADVISOR")
    void updateProduct_WrongOwnership_ThrowsResourceNotFound() {
        ProductUpdateDTO dto = new ProductUpdateDTO(
                "Hacked", BigDecimal.TEN, LocalDate.now(), null, dbType.getId(), dbProvider.getId()
        );

        // Advisor 1 trying to update Product managed by Advisor 2
        assertThrows(ResourceNotFoundException.class, () ->
                productService.updateProduct(productAdv2.getId(), dto, testAdvisor1.getEmployeeId())
        );
    }

    @Test
    @WithMockUser(username = "IT-ADV-1", authorities = "ADVISOR")
    void updateProduct_InvalidForeignKeys_ThrowsInvalidInputValue() {
        ProductUpdateDTO dto = new ProductUpdateDTO(
                "Bad Refs", BigDecimal.TEN, LocalDate.now(), null, 9999L, dbProvider.getId()
        );

        assertThrows(InvalidInputValueException.class, () ->
                productService.updateProduct(activeProductAdv1.getId(), dto, testAdvisor1.getEmployeeId())
        );
    }

    // --- 4. SEARCH PRODUCTS ---

    @Test
    @WithMockUser(username = "IT-ADM-1", authorities = "ADMIN")
    void searchProducts_AdminGlobalSearch_ReturnsAllClientProducts() {
        ProductSearchCriteriaDTO criteria = new ProductSearchCriteriaDTO(
                null, testClient1.getClientUid(), null, null, null, null
        );

        Page<ProductDTO> result = productService.searchProducts(criteria, Pageable.unpaged(), testAdmin.getEmployeeId(), true);

        // Should return all products associated with Client1
        assertTrue(result.getTotalElements() >= 3);
        assertTrue(result.getContent().stream().anyMatch(p -> p.id().equals(externalProduct.getId())));
    }

    @Test
    @WithMockUser(username = "IT-ADV-1", authorities = "ADVISOR")
    void searchProducts_AdvisorSearchOwnClient_ReturnsAllIncludingExternal() {
        ProductSearchCriteriaDTO criteria = new ProductSearchCriteriaDTO(
                null, testClient1.getClientUid(), null, null, null, null
        );

        Page<ProductDTO> result = productService.searchProducts(criteria, Pageable.unpaged(), testAdvisor1.getEmployeeId(), false);

        assertTrue(result.getTotalElements() >= 3);
        assertTrue(result.getContent().stream().anyMatch(p -> p.id().equals(externalProduct.getId())));
    }

    @Test
    @WithMockUser(username = "IT-ADV-1", authorities = "ADVISOR")
    void searchProducts_AdvisorSearchCrossClient_ReturnsOnlyManaged() {
        ProductSearchCriteriaDTO criteria = new ProductSearchCriteriaDTO(
                null, testClient2.getClientUid(), null, null, null, null
        );

        Page<ProductDTO> result = productService.searchProducts(criteria, Pageable.unpaged(), testAdvisor1.getEmployeeId(), false);

        assertEquals(1, result.getTotalElements());
        assertEquals(crossClientProduct.getId(), result.getContent().getFirst().id());
    }

    @Test
    @WithMockUser(username = "IT-ADV-1", authorities = "ADVISOR")
    void searchProducts_StatusFiltering_ReturnsCorrectRecords() {
        // Test EXPIRED
        ProductSearchCriteriaDTO expiredCriteria = new ProductSearchCriteriaDTO(
                null, testClient1.getClientUid(), null, null, null, ProductStatus.EXPIRED
        );
        Page<ProductDTO> expiredResult = productService.searchProducts(expiredCriteria, Pageable.unpaged(), testAdvisor1.getEmployeeId(), false);

        assertEquals(1, expiredResult.getTotalElements());
        assertEquals(expiredProductAdv1.getId(), expiredResult.getContent().getFirst().id());

        // Test ACTIVE
        ProductSearchCriteriaDTO activeCriteria = new ProductSearchCriteriaDTO(
                null, testClient1.getClientUid(), null, null, null, ProductStatus.ACTIVE
        );
        Page<ProductDTO> activeResult = productService.searchProducts(activeCriteria, Pageable.unpaged(), testAdvisor1.getEmployeeId(), false);

        assertTrue(activeResult.getTotalElements() >= 1);
        assertTrue(activeResult.getContent().stream().anyMatch(p -> p.id().equals(activeProductAdv1.getId())));
        assertFalse(activeResult.getContent().stream().anyMatch(p -> p.id().equals(expiredProductAdv1.getId())));
    }
}