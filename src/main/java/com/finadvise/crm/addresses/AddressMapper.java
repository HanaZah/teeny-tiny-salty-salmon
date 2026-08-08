package com.finadvise.crm.addresses;

import org.springframework.stereotype.Component;

@Component
class AddressMapper {

    public AddressDTO toDto(Address address) {
        if (address == null) {
            return null;
        }

        return new AddressDTO(
                address.getId(),
                address.getStreet(),
                address.getHouseNumber(),
                address.getCity(),
                address.getPostalCode()
        );
    }

    public Address toEntity(AddressInputDTO dto) {
        if (dto == null) {
            return null;
        }

        return Address.builder()
                .postalCode(dto.postalCode().trim())
                .city(dto.city().trim())
                .street(dto.street().trim())
                .houseNumber(dto.houseNumber().trim())
                .build();
    }
}