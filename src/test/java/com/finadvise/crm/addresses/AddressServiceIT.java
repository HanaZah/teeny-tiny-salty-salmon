package com.finadvise.crm.addresses;

import com.finadvise.crm.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AddressServiceIT extends AbstractIntegrationTest {

    @Autowired
    private AddressService addressService;

    @Autowired
    private AddressPersistHelper addressPersistHelper;

    @Autowired
    private AddressRepository addressRepository;

    @BeforeEach
    void setUp() {
        addressRepository.deleteAll();
    }

    @Test
    void findOrCreateAddress_NewAddress_PersistsToDatabase() {
        AddressInputDTO input = new AddressInputDTO("Dlouhá", "15", "Praha", "110 00");

        AddressDTO result = addressService.findOrCreateAddress(input);

        assertNotNull(result);
        assertNotNull(result.id());
        assertEquals("Dlouhá", result.street());

        Address dbAddress = addressRepository.findById(result.id()).orElseThrow();
        assertEquals("Praha", dbAddress.getCity());
        assertEquals("110 00", dbAddress.getPostalCode());
    }

    @Test
    void findOrCreateAddress_CaseInsensitiveDeduplication_ReturnsExisting() {
        AddressInputDTO initialInput = new AddressInputDTO("krátká", "10/1b", "Brno", "602 00");
        AddressDTO firstCreation = addressService.findOrCreateAddress(initialInput);

        AddressInputDTO caseVariantInput = new AddressInputDTO("KRÁTKÁ", "10/1B", "BRNO", "602 00");
        AddressDTO secondCreation = addressService.findOrCreateAddress(caseVariantInput);

        assertNotNull(firstCreation);
        assertNotNull(secondCreation);
        assertEquals(firstCreation.id(), secondCreation.id());
        assertEquals(1, addressRepository.count());
    }

    @Test
    void requiresNewTransaction_IsolatesExceptionsOnConstraintViolation() {
        Address address = Address.builder()
                .street("Testova")
                .houseNumber("1")
                .city("Ostrava")
                .postalCode("702 00")
                .build();

        addressRepository.saveAndFlush(address);

        Address duplicateAddress = Address.builder()
                .street("Testova")
                .houseNumber("1")
                .city("Ostrava")
                .postalCode("702 00")
                .build();

        assertThrows(DataIntegrityViolationException.class, () ->
                addressPersistHelper.saveAndFlushRequiresNew(duplicateAddress)
        );

        AddressInputDTO lookupDto = new AddressInputDTO("Testova", "1", "Ostrava", "702 00");
        AddressDTO recoveredDto = addressService.findOrCreateAddress(lookupDto);

        assertNotNull(recoveredDto);
        assertEquals(address.getId(), recoveredDto.id());
    }
}