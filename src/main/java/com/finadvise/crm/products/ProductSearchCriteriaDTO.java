package com.finadvise.crm.products;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProductSearchCriteriaDTO (
        @Size(max = 20, message = "product.search.advisor-id.size")
        @Pattern(
                regexp = "^[a-zA-Z0-9\\-]+$",
                message = "product.search.advisor-id.format"
        )
        String advisorEmployeeId,

        @Size(max = 20, message = "product.search.client-uid.size")
        @Pattern(
                regexp = "^[a-zA-Z0-9\\-]+$",
                message = "product.search.client-uid.format"
        )
        String clientUid,

        @Size(max = 150, message = "product.search.name.size")
        @Pattern(
                regexp = "^[\\p{L}\\p{M}\\d\\s\\-']+$",
                message = "product.search.name.format"
        )
        String productName,

        Long productTypeId,
        Long productProviderId,
        ProductStatus productStatus
){
        /**
         * Creates a secure copy, overriding the advisor ID
         * with the authenticated user's ID to prevent privilege escalation.
         */
        public ProductSearchCriteriaDTO withAdvisorEmployeeId(String secureEmployeeId) {
                return new ProductSearchCriteriaDTO(
                        secureEmployeeId,
                        this.clientUid,
                        this.productName,
                        this.productTypeId,
                        this.productProviderId,
                        this.productStatus
                );
        }
}