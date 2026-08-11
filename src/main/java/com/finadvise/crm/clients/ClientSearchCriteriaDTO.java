package com.finadvise.crm.clients;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ClientSearchCriteriaDTO(
        @Size(max = 20, message = "Employee ID has wrong size or pattern")
        @Pattern(
                regexp = "^[a-zA-Z0-9\\-]+$",
                message = "Employee ID has wrong size or pattern"
        )
        String advisorEmployeeId,

        @Size(max = 150, message = "Product name must be at most 150 characters long")
        @Pattern(
                regexp = "^[\\p{L}\\p{M}\\d\\s\\-']+$",
                message = "Product name contains invalid characters." +
                        "Please use only standard letters, digits and basic punctuation."
        )
        String name,

        @Size(max = 10, message = "Personal ID has wrong size or pattern")
        @Pattern(regexp = "\\d{10}", message = "Personal ID has wrong size or pattern")
        String personalId,

        @Size(max = 100, message = "City name must be at most 100 characters long")
        @Pattern(
                regexp = "^[\\p{L}\\p{M}\\d\\s\\-'.]+$",
                message = "City name contains invalid characters." +
                        "Please use only standard letters, digits and basic punctuation."
        )
        String city,
        ClientStatus status
){
        /**
         * Creates a secure copy, overriding the advisor ID
         * with the authenticated user's ID to prevent privilege escalation.
         */
        public ClientSearchCriteriaDTO withEmployeeId(String employeeId) {
            return new ClientSearchCriteriaDTO(employeeId, name, personalId, city, status);
        }
}