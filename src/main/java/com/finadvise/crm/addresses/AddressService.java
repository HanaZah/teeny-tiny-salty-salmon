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

    /**
     * Resolves an AddressDTO into a managed Address entity.
     * It looks up existing records to prevent unique constraint violations,
     * creating new ones only if they don't already exist.
     */
    @Override
    @Transactional(readOnly = true)
    public AddressDTO findOrCreateAddress(AddressInputDTO dto) {
        if (dto == null) {
            return null;
        }
        AddressInputDTO validatedAddress = addressValidator.validate(dto);

        return addressRepository.findExistingAddressCaseInsensitive(
                        validatedAddress.street().trim(),
                        validatedAddress.city().trim(),
                        validatedAddress.postalCode().trim(),
                        validatedAddress.houseNumber().trim()
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
            // A concurrent thread successfully inserted the exact same address
            // between our initial SELECT and our INSERT attempt.
            // The constraint violation is caught, and we safely fetch the newly inserted record.
            Address existingAddress = addressRepository.findExistingAddressCaseInsensitive(
                    dto.street().trim(),
                    dto.city().trim(),
                    dto.postalCode().trim(),
                    dto.houseNumber().trim()
            ).orElseThrow(() -> new SystemIntegrityException(
                    "Address recovery fetch failed after constraint violation.", e));

            return addressMapper.toDto(existingAddress);
        }
    }
}
