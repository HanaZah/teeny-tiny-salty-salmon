package com.finadvise.crm.products;

import com.finadvise.crm.TestFixtureFactory;
import com.finadvise.crm.clients.Client;
import com.finadvise.crm.clients.ClientReadFacade;
import com.finadvise.crm.clients.ClientSummaryDTO;
import com.finadvise.crm.common.InvalidInputValueException;
import com.finadvise.crm.common.ResourceNotFoundException;
import com.finadvise.crm.common.SystemIntegrityException;
import com.finadvise.crm.users.AdvisorSummaryDTO;
import com.finadvise.crm.users.User;
import com.finadvise.crm.users.UserReadFacade;
import com.finadvise.crm.users.UserType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private ProductTypeRepository productTypeRepository;
    @Mock private ProviderRepository providerRepository;
    @Mock private ProductMapper productMapper;
    @Mock private UserReadFacade userReadFacade;
    @Mock private ClientReadFacade clientReadFacade;
    @Mock private Clock clock;

    @InjectMocks
    private ProductService productService;

    private User mockAdvisor;
    private Client mockClient;
    private Product mockProduct;
    private ProductDTO mockProductDTO;
    private AdvisorSummaryDTO mockAdvisorSummary;
    private ClientSummaryDTO mockClientSummary;

    @BeforeEach
    void setUp() {
        mockAdvisor = TestFixtureFactory.createValidUser("EMP-123", UserType.ADVISOR);
        mockClient = TestFixtureFactory.createValidClient(1L, "CLI-123", mockAdvisor);
        mockProduct = TestFixtureFactory.createValidProduct(1L, mockClient, mockAdvisor);

        mockAdvisorSummary = new AdvisorSummaryDTO("EMP-123", "John", "Doe");
        mockClientSummary = new ClientSummaryDTO("C-123456", "John", "Smith");
        mockProductDTO = new ProductDTO(
                1L, "Test Product", new BigDecimal("5000.00"), LocalDate.of(2026, 1, 1),
                null, new ProductTypeDTO(1L, "Type"), new ProductProviderDTO(1L, "Provider"),
                mockAdvisorSummary, mockClientSummary
        );
    }

    private void mockClock() {
        Instant fixedInstant = Instant.parse("2026-08-09T10:00:00Z");
        when(clock.instant()).thenReturn(fixedInstant);
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));
    }

    // --- UPDATE PRODUCT TESTS ---

    @Test
    void updateProduct_FullUpdate_Success() {
        ProductUpdateDTO dto = new ProductUpdateDTO(
                "Updated Name", new BigDecimal("1000.00"), LocalDate.of(2026, 1, 1), null, 2L, 2L
        );

        ProductType newType = ProductType.builder().id(2L).name("New Type").build();
        Provider newProvider = Provider.builder().id(2L).name("New Provider").build();

        when(productRepository.findByIdAndAdvisor_EmployeeId(mockProduct.getId(), mockAdvisor.getEmployeeId()))
                .thenReturn(Optional.of(mockProduct));
        when(productTypeRepository.existsById(2L)).thenReturn(true);
        when(providerRepository.existsById(2L)).thenReturn(true);
        when(productTypeRepository.getReferenceById(2L)).thenReturn(newType);
        when(providerRepository.getReferenceById(2L)).thenReturn(newProvider);
        when(productRepository.saveAndFlush(any(Product.class))).thenReturn(mockProduct);
        when(productRepository.findByIdWithDetails(mockProduct.getId())).thenReturn(Optional.of(mockProduct));
        when(userReadFacade.mapToAdvisorSummary(any())).thenReturn(mockAdvisorSummary);
        when(clientReadFacade.mapToClientSummary(any())).thenReturn(mockClientSummary);
        when(productMapper.toDto(any(), any(), any())).thenReturn(mockProductDTO);

        ProductDTO result = productService.updateProduct(mockProduct.getId(), dto, mockAdvisor.getEmployeeId());

        assertNotNull(result);
        assertEquals(mockProductDTO, result);
        verify(productRepository).saveAndFlush(mockProduct);
        assertEquals("Updated Name", mockProduct.getName());
        assertEquals(newType, mockProduct.getProductType());
        assertEquals(newProvider, mockProduct.getProvider());
    }

    @Test
    void updateProduct_PartialUpdate_SuccessBypassesRepositoryChecks() {
        // Keeping same type and provider ID to bypass DB checks
        ProductUpdateDTO dto = new ProductUpdateDTO(
                "Updated Name", new BigDecimal("1000.00"), LocalDate.of(2026, 1, 1), null, 1L, 1L
        );

        when(productRepository.findByIdAndAdvisor_EmployeeId(mockProduct.getId(), mockAdvisor.getEmployeeId()))
                .thenReturn(Optional.of(mockProduct));
        when(productRepository.saveAndFlush(any(Product.class))).thenReturn(mockProduct);
        when(productRepository.findByIdWithDetails(mockProduct.getId())).thenReturn(Optional.of(mockProduct));
        when(productMapper.toDto(any(), any(), any())).thenReturn(mockProductDTO);

        ProductDTO result = productService.updateProduct(mockProduct.getId(), dto, mockAdvisor.getEmployeeId());

        assertNotNull(result);
        verify(productTypeRepository, never()).existsById(anyLong());
        verify(providerRepository, never()).existsById(anyLong());
    }

    @Test
    void updateProduct_NotFound_ThrowsException() {
        ProductUpdateDTO dto = new ProductUpdateDTO("Name", BigDecimal.TEN, LocalDate.now(), null, 1L, 1L);
        when(productRepository.findByIdAndAdvisor_EmployeeId(999L, "EMP-123")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                productService.updateProduct(999L, dto, "EMP-123")
        );
    }

    @Test
    void updateProduct_InvalidDateRange_ThrowsException() {
        ProductUpdateDTO dto = new ProductUpdateDTO(
                "Name", BigDecimal.TEN, LocalDate.of(2026, 1, 2), LocalDate.of(2026, 1, 1), 1L, 1L
        );
        when(productRepository.findByIdAndAdvisor_EmployeeId(mockProduct.getId(), mockAdvisor.getEmployeeId()))
                .thenReturn(Optional.of(mockProduct));

        assertThrows(InvalidInputValueException.class, () ->
                productService.updateProduct(mockProduct.getId(), dto, mockAdvisor.getEmployeeId())
        );
    }

    @Test
    void updateProduct_InvalidProductType_ThrowsException() {
        ProductUpdateDTO dto = new ProductUpdateDTO("Name", BigDecimal.TEN, LocalDate.now(), null, 99L, 1L);
        when(productRepository.findByIdAndAdvisor_EmployeeId(mockProduct.getId(), mockAdvisor.getEmployeeId()))
                .thenReturn(Optional.of(mockProduct));
        when(productTypeRepository.existsById(99L)).thenReturn(false);

        assertThrows(InvalidInputValueException.class, () ->
                productService.updateProduct(mockProduct.getId(), dto, mockAdvisor.getEmployeeId())
        );
    }

    @Test
    void updateProduct_InvalidProvider_ThrowsException() {
        ProductUpdateDTO dto = new ProductUpdateDTO("Name", BigDecimal.TEN, LocalDate.now(), null, 1L, 99L);
        when(productRepository.findByIdAndAdvisor_EmployeeId(mockProduct.getId(), mockAdvisor.getEmployeeId()))
                .thenReturn(Optional.of(mockProduct));
        when(providerRepository.existsById(99L)).thenReturn(false);

        assertThrows(InvalidInputValueException.class, () ->
                productService.updateProduct(mockProduct.getId(), dto, mockAdvisor.getEmployeeId())
        );
    }

    // --- SEARCH PRODUCTS TESTS ---

    @Test
    void searchProducts_Admin_PassesCriteriaIntact() {
        mockClock();
        ProductSearchCriteriaDTO criteria = new ProductSearchCriteriaDTO("OTHER-EMP", null, null, null, null, null);
        Page<Product> productPage = new PageImpl<>(List.of(mockProduct));

        when(productRepository.findAll(ArgumentMatchers.<Specification<Product>>any(), any(Pageable.class))).thenReturn(productPage);
        when(productMapper.toDto(any(), any(), any())).thenReturn(mockProductDTO);

        Page<ProductDTO> result = productService.searchProducts(criteria, Pageable.unpaged(), "ADM-1", true);

        assertEquals(1, result.getTotalElements());
        assertEquals(mockProductDTO, result.getContent().getFirst());
        verify(productRepository).findAll(ArgumentMatchers.<Specification<Product>>any(), any(Pageable.class));
    }

    @Test
    void searchProducts_Advisor_OverridesEmployeeId() {
        mockClock();
        // Advisor tries to search for another advisor's products
        ProductSearchCriteriaDTO criteria = new ProductSearchCriteriaDTO("SNEAKY-EMP", null, null, null, null, null);
        Page<Product> productPage = new PageImpl<>(List.of(mockProduct));

        when(productRepository.findAll(ArgumentMatchers.<Specification<Product>>any(), any(Pageable.class))).thenReturn(productPage);
        when(productMapper.toDto(any(), any(), any())).thenReturn(mockProductDTO);

        Page<ProductDTO> result = productService.searchProducts(criteria, Pageable.unpaged(), "EMP-123", false);

        assertEquals(1, result.getTotalElements());
        // Specification internally uses the modified DTO. Verifying behavior through successful mapping.
        verify(productRepository).findAll(ArgumentMatchers.<Specification<Product>>any(), any(Pageable.class));
    }

    @Test
    void searchProducts_EmptyResult_ReturnsEmptyPage() {
        mockClock();
        ProductSearchCriteriaDTO criteria = new ProductSearchCriteriaDTO(null, null, null, null, null, null);
        when(productRepository.findAll(ArgumentMatchers.<Specification<Product>>any(), any(Pageable.class))).thenReturn(Page.empty());

        Page<ProductDTO> result = productService.searchProducts(criteria, Pageable.unpaged(), "EMP-123", false);

        assertTrue(result.isEmpty());
    }

    // --- CREATE PRODUCT TESTS ---

    @Test
    void createProduct_InternalProduct_Success() {
        mockClock();
        ProductCreateDTO dto = new ProductCreateDTO(
                "New Product", BigDecimal.TEN, LocalDate.now(clock), null, 1L, 1L, false
        );

        when(userReadFacade.findByEmployeeId(mockAdvisor.getEmployeeId())).thenReturn(Optional.of(mockAdvisor));
        when(clientReadFacade.findByClientUidAndAdvisorEmployeeId(mockClient.getClientUid(), mockAdvisor.getEmployeeId()))
                .thenReturn(Optional.of(mockClient));
        when(productTypeRepository.existsById(1L)).thenReturn(true);
        when(providerRepository.existsById(1L)).thenReturn(true);
        when(productTypeRepository.getReferenceById(1L)).thenReturn(mockProduct.getProductType());
        when(providerRepository.getReferenceById(1L)).thenReturn(mockProduct.getProvider());
        when(productRepository.saveAndFlush(any(Product.class))).thenReturn(mockProduct);
        when(productRepository.findByIdWithDetails(any())).thenReturn(Optional.of(mockProduct));
        when(productMapper.toDto(any(), any(), any())).thenReturn(mockProductDTO);

        ProductDTO result = productService.createProduct(dto, mockAdvisor.getEmployeeId(), mockClient.getClientUid());

        assertNotNull(result);
        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).saveAndFlush(captor.capture());
        Product savedProduct = captor.getValue();
        assertEquals(mockAdvisor, savedProduct.getAdvisor());
    }

    @Test
    void createProduct_ExternalProduct_Success() {
        mockClock();
        ProductCreateDTO dto = new ProductCreateDTO(
                "External Product", BigDecimal.TEN, LocalDate.now(clock), null, 1L, 1L, true // isExternal = true
        );

        when(userReadFacade.findByEmployeeId(mockAdvisor.getEmployeeId())).thenReturn(Optional.of(mockAdvisor));
        when(clientReadFacade.findByClientUidAndAdvisorEmployeeId(mockClient.getClientUid(), mockAdvisor.getEmployeeId()))
                .thenReturn(Optional.of(mockClient));
        when(productTypeRepository.existsById(1L)).thenReturn(true);
        when(providerRepository.existsById(1L)).thenReturn(true);
        when(productTypeRepository.getReferenceById(1L)).thenReturn(mockProduct.getProductType());
        when(providerRepository.getReferenceById(1L)).thenReturn(mockProduct.getProvider());
        when(productRepository.saveAndFlush(any(Product.class))).thenReturn(mockProduct);
        when(productRepository.findByIdWithDetails(any())).thenReturn(Optional.of(mockProduct));

        productService.createProduct(dto, mockAdvisor.getEmployeeId(), mockClient.getClientUid());

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).saveAndFlush(captor.capture());
        Product savedProduct = captor.getValue();
        assertNull(savedProduct.getAdvisor()); // Manager must be null for external products
    }

    @Test
    void createProduct_AdvisorMissing_ThrowsException() {
        ProductCreateDTO dto = new ProductCreateDTO("Name", BigDecimal.TEN, LocalDate.now(), null, 1L, 1L, false);
        when(userReadFacade.findByEmployeeId("UNKNOWN")).thenReturn(Optional.empty());

        assertThrows(SystemIntegrityException.class, () ->
                productService.createProduct(dto, "UNKNOWN", "C-123")
        );
    }

    @Test
    void createProduct_ClientNotFound_ThrowsException() {
        ProductCreateDTO dto = new ProductCreateDTO("Name", BigDecimal.TEN, LocalDate.now(), null, 1L, 1L, false);
        when(userReadFacade.findByEmployeeId(mockAdvisor.getEmployeeId())).thenReturn(Optional.of(mockAdvisor));
        when(clientReadFacade.findByClientUidAndAdvisorEmployeeId("UNKNOWN", mockAdvisor.getEmployeeId()))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                productService.createProduct(dto, mockAdvisor.getEmployeeId(), "UNKNOWN")
        );
    }

    @Test
    void createProduct_ClientIneligible_ThrowsException() {
        mockClock();
        ProductCreateDTO dto = new ProductCreateDTO("Name", BigDecimal.TEN, LocalDate.now(clock), null, 1L, 1L, false);

        mockClient.setIdCardExpiryDate(LocalDate.of(2020, 1, 1)); // Expired ID card

        when(userReadFacade.findByEmployeeId(mockAdvisor.getEmployeeId())).thenReturn(Optional.of(mockAdvisor));
        when(clientReadFacade.findByClientUidAndAdvisorEmployeeId(mockClient.getClientUid(), mockAdvisor.getEmployeeId()))
                .thenReturn(Optional.of(mockClient));

        assertThrows(InvalidInputValueException.class, () ->
                productService.createProduct(dto, mockAdvisor.getEmployeeId(), mockClient.getClientUid())
        );
    }

    @Test
    void createProduct_InvalidDateRange_ThrowsException() {
        mockClock();
        ProductCreateDTO dto = new ProductCreateDTO(
                "Name", BigDecimal.TEN, LocalDate.of(2026, 1, 2), LocalDate.of(2026, 1, 1), 1L, 1L, false
        );

        when(userReadFacade.findByEmployeeId(mockAdvisor.getEmployeeId())).thenReturn(Optional.of(mockAdvisor));
        when(clientReadFacade.findByClientUidAndAdvisorEmployeeId(mockClient.getClientUid(), mockAdvisor.getEmployeeId()))
                .thenReturn(Optional.of(mockClient));

        assertThrows(InvalidInputValueException.class, () ->
                productService.createProduct(dto, mockAdvisor.getEmployeeId(), mockClient.getClientUid())
        );
    }

    @Test
    void createProduct_InvalidProductType_ThrowsException() {
        mockClock();
        ProductCreateDTO dto = new ProductCreateDTO("Name", BigDecimal.TEN, LocalDate.now(clock), null, 99L, 1L, false);

        when(userReadFacade.findByEmployeeId(mockAdvisor.getEmployeeId())).thenReturn(Optional.of(mockAdvisor));
        when(clientReadFacade.findByClientUidAndAdvisorEmployeeId(mockClient.getClientUid(), mockAdvisor.getEmployeeId()))
                .thenReturn(Optional.of(mockClient));
        when(productTypeRepository.existsById(99L)).thenReturn(false);

        assertThrows(InvalidInputValueException.class, () ->
                productService.createProduct(dto, mockAdvisor.getEmployeeId(), mockClient.getClientUid())
        );
    }

    @Test
    void createProduct_InvalidProvider_ThrowsException() {
        mockClock();
        ProductCreateDTO dto = new ProductCreateDTO("Name", BigDecimal.TEN, LocalDate.now(clock), null, 1L, 99L, false);

        when(userReadFacade.findByEmployeeId(mockAdvisor.getEmployeeId())).thenReturn(Optional.of(mockAdvisor));
        when(clientReadFacade.findByClientUidAndAdvisorEmployeeId(mockClient.getClientUid(), mockAdvisor.getEmployeeId()))
                .thenReturn(Optional.of(mockClient));
        when(productTypeRepository.existsById(1L)).thenReturn(true);
        when(providerRepository.existsById(99L)).thenReturn(false);

        assertThrows(InvalidInputValueException.class, () ->
                productService.createProduct(dto, mockAdvisor.getEmployeeId(), mockClient.getClientUid())
        );
    }

    // --- GET PRODUCTS STATISTICS TESTS ---

    @Test
    void getProductsStatisticsForClient_Success_DelegatesToRepository() {
        mockClock();
        ProductsStatisticsDTO expectedStats = new ProductsStatisticsDTO(5L, 3L, 2L, new BigDecimal("1500.00"));

        when(productRepository.getClientProductStatistics(mockClient.getClientUid(), mockAdvisor.getEmployeeId(), LocalDate.now(clock)))
                .thenReturn(expectedStats);

        ProductsStatisticsDTO result = productService.getProductsStatisticsForClient(mockClient.getClientUid(), mockAdvisor.getEmployeeId());

        assertNotNull(result);
        assertEquals(expectedStats, result);

        // Verifies the clock output was properly extracted and passed to the repository query
        verify(productRepository).getClientProductStatistics(mockClient.getClientUid(), mockAdvisor.getEmployeeId(), LocalDate.of(2026, 8, 9));
    }
}