package com.finadvise.crm.addresses;

public record AddressDTO(
        Long id,
        String street,
        String houseNumber,
        String city,
        String postalCode
) {}
