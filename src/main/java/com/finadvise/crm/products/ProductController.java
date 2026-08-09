package com.finadvise.crm.products;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @Operation(summary = "Update product", description = "Owning-advisor-restricted endpoint for product update.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed, incorrect payload format"),
            @ApiResponse(responseCode = "404", description = "Product not found or access denied"),

    })
    @PreAuthorize("hasAuthority('ADVISOR')")
    @PutMapping("/{id}")
    public ProductDTO updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductUpdateDTO dto,
            Principal principal) {

        return productService.updateProduct(id, dto, principal.getName());
    }

    @Operation(summary = "Search products", description = "Authenticated user endpoint for product search.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Products found successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed, incorrect payload format"),
    })
    @PostMapping("/search")
    @PreAuthorize("hasAnyAuthority('ADVISOR', 'ADMIN')")
    public Page<ProductDTO> searchProducts(
            @Valid @RequestBody ProductSearchCriteriaDTO criteria,
            Pageable pageable,
            Authentication authentication) {

        return productService .searchProducts(criteria, pageable, authentication.getName(), isAdmin(authentication));
    }

    private boolean isAdmin(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ADMIN"::equals);
    }
}
