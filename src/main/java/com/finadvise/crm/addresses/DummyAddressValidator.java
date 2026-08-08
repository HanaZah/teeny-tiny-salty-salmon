package com.finadvise.crm.addresses;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!prod")
class DummyAddressValidator implements ExternalAddressValidator{
    @Override
    public void validate(AddressInputDTO address) {
        // Empty dummy, in dev we relay on partial validation in DTO
    }
}
