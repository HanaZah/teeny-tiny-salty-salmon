package com.finadvise.crm.addresses;

interface ExternalAddressValidator {
    /**
     * Validates and formats the address via external API like national registry (RÚIAN).
     * Throws AddressValidationException if no such address exists.
     */
    AddressInputDTO validate(AddressInputDTO address);
}
