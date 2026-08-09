package com.finadvise.crm.products;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProductSearchCriteriaDTO (
        @Size(max = 20, message = "Employee ID has wrong size or pattern")
        @Pattern(
                regexp = "^[a-zA-Z0-9\\-]+$",
                message = "Employee ID has wrong size or pattern"
        )
        String advisorEmployeeId,

        @Size(max = 20, message = "Client UID has wrong size or pattern")
        @Pattern(
                regexp = "^[a-zA-Z0-9\\-]+$",
                message = "Client UID has wrong size or pattern"
        )
        String clientUid,

        @Size(max = 150, message = "Product name must be at most 150 characters long")
        @Pattern(
                regexp = "^[\\p{L}\\p{M}\\d\\s\\-']+$",
                message = "Product name contains invalid characters." +
                        "Please use only standard letters, digits and basic punctuation."
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