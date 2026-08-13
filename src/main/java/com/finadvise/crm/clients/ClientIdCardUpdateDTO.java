package com.finadvise.crm.clients;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ClientIdCardUpdateDTO(
        @NotNull(message = "client.version.required")
        Integer version,

        @NotBlank(message = "client.id-card-number.required")
        @Size(min = 9, max = 9, message = "client.id-card-number.size")
        @Pattern(regexp = "^\\d{9}$", message = "client.id-card-number.format")
        String idCardNumber,

        @NotNull(message = "client.id-card-issue-date.required")
        LocalDate idCardIssueDate,

        @NotNull(message = "client.id-card-expiry-date.required")
        LocalDate idCardExpiryDate,

        @NotBlank(message = "client.id-card-issuer.required")
        @Size(max = 100, message = "client.id-card-issuer.size")
        @Pattern(regexp = "^[\\p{L}\\p{M}\\d\\s\\-'.]+$", message = "client.id-card-issuer.format")
        String idCardIssuer
) {}