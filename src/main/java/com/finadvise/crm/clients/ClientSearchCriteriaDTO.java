package com.finadvise.crm.clients;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ClientSearchCriteriaDTO(
        @Size(max = 20, message = "client.search.advisor-id.size")
        @Pattern(
                regexp = "^[a-zA-Z0-9\\-]+$",
                message = "client.search.advisor-id.format"
        )
        String advisorEmployeeId,

        @Size(max = 150, message = "client.search.name.size")
        @Pattern(
                regexp = "^[\\p{L}\\p{M}\\d\\s\\-']+$",
                message = "client.search.name.format"
        )
        String name,

        @Size(max = 10, message = "client.search.personal-id.size")
        @Pattern(regexp = "\\d{10}", message = "client.search.personal-id.format")
        String personalId,

        @Size(max = 100, message = "client.search.city.size")
        @Pattern(
                regexp = "^[\\p{L}\\p{M}\\d\\s\\-'.]+$",
                message = "client.search.city.format"
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