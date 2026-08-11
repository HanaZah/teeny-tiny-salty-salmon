package com.finadvise.crm.clients;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ClientIdCardUpdateDTO(
        @NotNull(message = "Version is required for concurrency control")
        Integer version,

        @NotBlank(message = "ID card number is required")
        @Size(min = 9, max = 9, message = "ID card number must be exactly 9 characters long")
        @Pattern(regexp = "^\\d{9}$", message = "ID card number must consist of exactly 9 digits")
        String idCardNumber,

        @NotNull(message = "ID card issue date is required")
        LocalDate idCardIssueDate,

        @NotNull(message = "ID card expiry date is required")
        LocalDate idCardExpiryDate,

        @NotBlank(message = "ID card issuer is required")
        @Size(max = 100, message = "ID card issuer must be at most 100 characters long")
        @Pattern(regexp = "^[\\p{L}\\p{M}\\d\\s\\-'.]+$", message = "ID card issuer contains invalid characters. " +
                "Please use only standard letters, digits and basic punctuation.")
        String idCardIssuer
) {}