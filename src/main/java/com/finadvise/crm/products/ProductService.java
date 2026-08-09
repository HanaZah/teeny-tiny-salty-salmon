package com.finadvise.crm.products;

import com.finadvise.crm.clients.Client;
import com.finadvise.crm.clients.ClientReadFacade;
import com.finadvise.crm.common.InvalidInputValueException;
import com.finadvise.crm.common.ResourceNotFoundException;
import com.finadvise.crm.common.SystemIntegrityException;
import com.finadvise.crm.users.User;
import com.finadvise.crm.users.UserReadFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
class ProductService {
    private final ProductRepository productRepository;
    private final ProductTypeRepository productTypeRepository;
    private final ProviderRepository providerRepository;
    private final ProductMapper productMapper;
    private final UserReadFacade userReadFacade;
    private final ClientReadFacade clientReadFacade;
    private final Clock clock;

    @PreAuthorize( "hasAuthority('ADVISOR') and #employeeId == authentication.name")
    @Transactional
    public ProductDTO updateProduct(Long productId, ProductUpdateDTO dto, String employeeId) {
        Product product = productRepository.findByIdAndAdvisor_EmployeeId(productId, employeeId).orElseThrow(
                () -> new ResourceNotFoundException("Product not found or access denied")
        );

        if (dto.endDate() != null && dto.endDate().isBefore(dto.startDate())) {
            throw new InvalidInputValueException("End date must be after start date");
        }

        if (!dto.productTypeId().equals(product.getProductType().getId())
                && !productTypeRepository.existsById(dto.productTypeId())) {
            throw new InvalidInputValueException("Product type not found");
        }

        if (!dto.productProviderId().equals(product.getProvider().getId())
                && !providerRepository.existsById(dto.productProviderId()) ) {
            throw new InvalidInputValueException("Product provider not found");
        }

        product.setName(dto.name());
        product.setAmount(dto.amount());
        product.setStartDate(dto.startDate());
        product.setEndDate(dto.endDate());

        if (!dto.productTypeId().equals(product.getProductType().getId())) {
            product.setProductType(productTypeRepository.getReferenceById(dto.productTypeId()));
        }

        if (!dto.productProviderId().equals(product.getProvider().getId())) {
            product.setProvider(providerRepository.getReferenceById(dto.productProviderId()));
        }

        productRepository.saveAndFlush(product);
        product = productRepository.findByIdWithDetails(product.getId()).orElseThrow(); //this will never throw, we just saved

        return productMapper.toDto(product,
                userReadFacade.mapToAdvisorSummary(product.getAdvisor()),
                clientReadFacade.mapToClientSummary(product.getClient()));
    }

    @PreAuthorize("(hasAuthority('ADMIN') and #isAdmin) or (hasAuthority('ADVISOR') and !#isAdmin)")
    @Transactional(readOnly = true)
    public Page<ProductDTO> searchProducts(
            ProductSearchCriteriaDTO criteria, Pageable pageable, String employeeId, boolean isAdmin) {
        ProductSearchCriteriaDTO secureCriteria = criteria;

        if (!isAdmin) {
            secureCriteria = criteria.withAdvisorEmployeeId(employeeId);
        }

        Specification<Product> spec = ProductSpecification.build(secureCriteria, LocalDate.now(clock));

        return productRepository.findAll(spec, pageable)
                .map(product -> productMapper.toDto(
                        product,
                        userReadFacade.mapToAdvisorSummary(product.getAdvisor()),
                        clientReadFacade.mapToClientSummary(product.getClient())
                ));
    }

    @PreAuthorize("hasAuthority('ADVISOR') and #employeeId == authentication.name")
    @Transactional
    public ProductDTO createProduct(ProductCreateDTO dto, String employeeId, String clientUid) {
        User advisor = userReadFacade.findByEmployeeId(employeeId).orElseThrow(
                () -> new SystemIntegrityException("Critical system failure: " +
                        "Authenticated user record is missing from the database")
        );

        User productManager = dto.isExternal()? null : advisor;

        Client client = clientReadFacade.findByClientUidAndAdvisorEmployeeId(clientUid, employeeId).orElseThrow(
                () -> new ResourceNotFoundException("Client not found or access denied")
        );

        client.validateEligibilityForNewProduct(LocalDate.now(clock));

        if (dto.endDate() != null && dto.endDate().isBefore(dto.startDate())) {
            throw new InvalidInputValueException("End date must be after start date");
        }

        if (!productTypeRepository.existsById(dto.productTypeId())) {
            throw new InvalidInputValueException("Product type not found");
        }

        if (!providerRepository.existsById(dto.productProviderId()) ) {
            throw new InvalidInputValueException("Product provider not found");
        }

        Product product = Product.builder()
                .name(dto.name())
                .amount(dto.amount())
                .startDate(dto.startDate())
                .endDate(dto.endDate())
                .productType(productTypeRepository.getReferenceById(dto.productTypeId()))
                .provider(providerRepository.getReferenceById(dto.productProviderId()))
                .advisor(productManager)
                .client(client)
                .build();

        productRepository.saveAndFlush(product);
        product = productRepository.findByIdWithDetails(product.getId()).orElseThrow();

        return productMapper.toDto(
                product,
                userReadFacade.mapToAdvisorSummary(product.getAdvisor()),
                clientReadFacade.mapToClientSummary(product.getClient()));
    }
}
