package com.finadvise.crm.addresses;

public interface AddressFacade {
    AddressDTO findOrCreateAddress(AddressInputDTO dto);
    AddressDTO mapToDto(Address address);
}
