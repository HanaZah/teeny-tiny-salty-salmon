package com.finadvise.crm.addresses;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private AddressMapper addressMapper;

    @Mock
    private ExternalAddressValidator addressValidator;

    @Mock
    private AddressPersistHelper addressPersistHelper;

    @InjectMocks
    private AddressService addressService;

    private AddressInputDTO inputDTO;
    private Address mockAddress;
    private AddressDTO expectedDTO;

    @BeforeEach
    void setUp() {
        inputDTO = new AddressInputDTO("Main Street", "123/A", "Prague", "110 00");
        mockAddress = Address.builder()
                .id(1L)
                .street("Main Street")
                .houseNumber("123/A")
                .city("Prague")
                .postalCode("110 00")
                .build();
        expectedDTO = new AddressDTO(1L, "Main Street", "123/A", "Prague", "110 00");
    }

    @Test
    void findOrCreateAddress_NullDto_ReturnsNull() {
        AddressDTO result = addressService.findOrCreateAddress(null);

        assertNull(result);
        verifyNoInteractions(addressValidator, addressRepository, addressMapper, addressPersistHelper);
    }

    @Test
    void findOrCreateAddress_ValidatorThrows_BubblesException() {
        doThrow(new AddressValidationException("Invalid address"))
                .when(addressValidator).validate(inputDTO);

        assertThrows(AddressValidationException.class, () ->
                addressService.findOrCreateAddress(inputDTO)
        );

        verifyNoInteractions(addressRepository, addressMapper, addressPersistHelper);
    }

    @Test
    void findOrCreateAddress_ExistingFound_ReturnsMappedDto() {
        when(addressRepository.findExistingAddressCaseInsensitive(
                "Main Street", "Prague", "110 00", "123/A"
        )).thenReturn(Optional.of(mockAddress));

        when(addressMapper.toDto(mockAddress)).thenReturn(expectedDTO);

        AddressDTO result = addressService.findOrCreateAddress(inputDTO);

        assertNotNull(result);
        assertEquals(1L, result.id());
        verifyNoInteractions(addressPersistHelper);
    }

    @Test
    void findOrCreateAddress_NotFound_CreatesAndReturnsDto() {
        when(addressRepository.findExistingAddressCaseInsensitive(
                "Main Street", "Prague", "110 00", "123/A"
        )).thenReturn(Optional.empty());

        when(addressMapper.toEntity(inputDTO)).thenReturn(mockAddress);
        when(addressPersistHelper.saveAndFlushRequiresNew(mockAddress)).thenReturn(mockAddress);
        when(addressMapper.toDto(mockAddress)).thenReturn(expectedDTO);

        AddressDTO result = addressService.findOrCreateAddress(inputDTO);

        assertNotNull(result);
        assertEquals(1L, result.id());
        verify(addressPersistHelper).saveAndFlushRequiresNew(mockAddress);
    }

    @Test
    void findOrCreateAddress_ConcurrentInsert_RecoversAndReturnsDto() {
        when(addressRepository.findExistingAddressCaseInsensitive(
                "Main Street", "Prague", "110 00", "123/A"
        ))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(mockAddress));

        when(addressMapper.toEntity(inputDTO)).thenReturn(mockAddress);
        when(addressPersistHelper.saveAndFlushRequiresNew(mockAddress))
                .thenThrow(new DataIntegrityViolationException("Unique constraint violated"));
        when(addressMapper.toDto(mockAddress)).thenReturn(expectedDTO);

        AddressDTO result = addressService.findOrCreateAddress(inputDTO);

        assertNotNull(result);
        assertEquals(1L, result.id());
        verify(addressRepository, times(2))
                .findExistingAddressCaseInsensitive(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void findOrCreateAddress_ConcurrentInsertRecoveryFails_ThrowsIllegalStateException() {
        when(addressRepository.findExistingAddressCaseInsensitive(
                "Main Street", "Prague", "110 00", "123/A"
        )).thenReturn(Optional.empty());

        when(addressMapper.toEntity(inputDTO)).thenReturn(mockAddress);
        when(addressPersistHelper.saveAndFlushRequiresNew(mockAddress))
                .thenThrow(new DataIntegrityViolationException("Unique constraint violated"));

        assertThrows(IllegalStateException.class, () ->
                addressService.findOrCreateAddress(inputDTO)
        );
        verify(addressRepository, times(2))
                .findExistingAddressCaseInsensitive(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void mapToDto_DelegatesToMapper() {
        when(addressMapper.toDto(mockAddress)).thenReturn(expectedDTO);

        AddressDTO result = addressService.mapToDto(mockAddress);

        assertNotNull(result);
        assertEquals(expectedDTO, result);
        verify(addressMapper).toDto(mockAddress);
    }
}