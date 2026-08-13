package com.finadvise.crm.addresses;

import com.finadvise.crm.common.SystemIntegrityException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class AddressService implements AddressFacade {

    private final AddressRepository addressRepository;
    private final AddressMapper addressMapper;
    private final ExternalAddressValidator addressValidator;
    private final AddressPersistHelper addressPersistHelper;

    @Override
    @Transactional(readOnly = true)
    public AddressDTO findOrCreateAddress(AddressInputDTO dto) {
        if (dto == null) {
            return null;
        }
        addressValidator.validate(dto);

        return addressRepository.findExistingAddressCaseInsensitive(
                        dto.street().trim(),
                        dto.city().trim(),
                        dto.postalCode().trim(),
                        dto.houseNumber().trim()
                ).map(addressMapper::toDto)
                .orElseGet(() -> attemptCreateWithFallback(dto));
    }

    @Override
    public AddressDTO mapToDto(Address address) {
        return addressMapper.toDto(address);
    }

    @Override
    @Transactional(readOnly = true)
    public Address getReferenceById(Long id) {
        return addressRepository.getReferenceById(id);
    }

    private AddressDTO attemptCreateWithFallback(AddressInputDTO dto) {
        try {
            Address newAddress = addressMapper.toEntity(dto);
            Address savedAddress = addressPersistHelper.saveAndFlushRequiresNew(newAddress);
            return addressMapper.toDto(savedAddress);
        } catch (DataIntegrityViolationException e) {
            // Handles the edge case where the address is concurrently inserted by another thread
            Address existingAddress = addressRepository.findExistingAddressCaseInsensitive(
                    dto.street(),
                    dto.city(),
                    dto.postalCode(),
                    dto.houseNumber()
            ).orElseThrow(() -> new SystemIntegrityException(
                    "error.address.recovery-failed", e));

            return addressMapper.toDto(existingAddress);
        }
    }
}