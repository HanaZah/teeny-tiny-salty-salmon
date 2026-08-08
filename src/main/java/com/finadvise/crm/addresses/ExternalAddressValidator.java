package com.finadvise.crm.addresses;

interface ExternalAddressValidator {
    /**
     * Validates if the address exists in the national registry (RÚIAN).
     * Throws InvalidAddressException if the address is not found.
     */
    void validate(AddressInputDTO address);
}
