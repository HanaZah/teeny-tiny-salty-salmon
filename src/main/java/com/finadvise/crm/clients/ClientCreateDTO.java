package com.finadvise.crm.clients;

import com.finadvise.crm.addresses.AddressInputDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record ClientCreateDTO(
        @NotBlank(message = "client.personal-id.required")
        @Size(min = 10, max = 10, message = "client.personal-id.size")
        @Pattern(regexp = "^\\d{10}$", message = "client.personal-id.format")
        String personalId,

        @NotNull(message = "client.birth-date.required")
        LocalDate birthDate,

        @NotBlank(message = "client.first-name.required")
        @Size(max = 50, message = "client.first-name.size")
        @Pattern(regexp = "^[\\p{L}\\p{M}\\d\\s\\-'.]+$", message = "client.first-name.format")
        String firstName,

        @NotBlank(message = "client.last-name.required")
        @Size(max = 50, message = "client.last-name.size")
        @Pattern(regexp = "^[\\p{L}\\p{M}\\d\\s\\-'.]+$", message = "client.last-name.format")
        String lastName,

        @NotBlank(message = "client.occupation.required")
        @Size(max = 100, message = "client.occupation.size")
        @Pattern(regexp = "^[\\p{L}\\p{M}\\d\\s\\-'.,]+$", message = "client.occupation.format")
        String occupation,

        @NotBlank(message = "client.phone.required")
        @Size(max = 20, message = "client.phone.size")
        @Pattern(regexp = "^\\+?[\\d\\s\\-]+$", message = "client.phone.format")
        String phone,

        @NotBlank(message = "client.email.required")
        @Size(max = 254, message = "client.email.size")
        @Email(message = "client.email.format")
        String email,

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
        String idCardIssuer,

        @NotNull(message = "client.residential-address.required")
        @Valid
        AddressInputDTO residentialAddress,

        @NotNull(message = "client.contact-address.required")
        @Valid
        AddressInputDTO contactAddress
) {}