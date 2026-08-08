package com.finadvise.crm.addresses;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class AddressPersistHelper {
    private final AddressRepository addressRepository;

    /**
     * Executes the save in a completely independent transaction.
     * If this fails and rolls back, it does not taint the parent transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Address saveAndFlushRequiresNew(Address address) {
        return addressRepository.saveAndFlush(address);
    }
}
