package com.finadvise.crm.products;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/clients/{clientUid}/products")
@RequiredArgsConstructor
public class ClientProductController {

    private final ProductService productService;

    @Operation(summary = "Create a new product", description = "Owning-advisor-restricted endpoint for product creation.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Product created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed, incorrect payload format"),
            @ApiResponse(responseCode = "404", description = "Client not found or access denied"),
            @ApiResponse(responseCode = "500", description = "Critical failure, authenticated user record missing")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ADVISOR')")
    // Intentionally omitting the ResponseEntity wrapper with the 'Location' header here
    // Current app version does not contain the product detail endpoint, and I'm not arbitrarily adding it just for RFC compliance
    public ProductDTO createProduct(
            @PathVariable String clientUid,
            @Valid @RequestBody ProductCreateDTO dto,
            Principal principal) {

        return productService.createProduct(dto, principal.getName(), clientUid);
    }
}
