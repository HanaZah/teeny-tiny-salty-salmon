package com.finadvise.crm.clients;

import com.finadvise.crm.addresses.AddressInputDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record ClientGeneralUpdateDTO(
        @NotNull(message = "Version is required for concurrency control")
        Integer version,

        @NotBlank(message = "Personal ID is required")
        @Size(min = 10, max = 10, message = "Personal ID must be exactly 10 characters long")
        @Pattern(regexp = "^\\d{10}$", message = "Personal ID must consist of exactly 10 digits")
        String personalId,

        @NotNull(message = "Birth date is required")
        LocalDate birthDate,

        @NotBlank(message = "First name is required")
        @Size(max = 50, message = "First name must be at most 50 characters long")
        @Pattern(regexp = "^[\\p{L}\\p{M}\\d\\s\\-'.]+$", message = "First name contains invalid characters. " +
                "Please use only standard letters, digits and basic punctuation.")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 50, message = "Last name must be at most 50 characters long")
        @Pattern(regexp = "^[\\p{L}\\p{M}\\d\\s\\-'.]+$", message = "Last name contains invalid characters. " +
                "Please use only standard letters, digits and basic punctuation.")
        String lastName,

        @NotBlank(message = "Occupation is required")
        @Size(max = 100, message = "Occupation must be at most 100 characters long")
        @Pattern(regexp = "^[\\p{L}\\p{M}\\d\\s\\-'.]+$", message = "Occupation contains invalid characters. " +
                "Please use only standard letters, digits and basic punctuation.")
        String occupation,

        @NotBlank(message = "Phone number is required")
        @Size(max = 20, message = "Phone number must be at most 20 characters long")
        @Pattern(regexp = "^\\+?[\\d\\s\\-]+$", message = "Invalid phone format.")
        String phone,

        @NotBlank(message = "Email is required")
        @Size(max = 254, message = "Email must be at most 254 characters long")
        @Email(message = "Must be a valid email format")
        String email,

        @NotNull(message = "Residential address is required")
        @Valid
        AddressInputDTO residentialAddress,

        @NotNull(message = "Contact address is required")
        @Valid
        AddressInputDTO contactAddress
) {}