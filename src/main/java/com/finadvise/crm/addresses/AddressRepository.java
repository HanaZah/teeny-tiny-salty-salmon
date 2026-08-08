package com.finadvise.crm.addresses;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {

    @Query("SELECT a FROM Address a " +
           "WHERE LOWER(a.street) = LOWER(:street) " +
           "AND LOWER(a.city) = LOWER(:city) " +
           "AND LOWER(a.postalCode) = LOWER(:postalCode) " +
           "AND LOWER(a.houseNumber) = LOWER(:houseNumber)")
    Optional<Address> findExistingAddressCaseInsensitive(
            @Param("street") String street,
            @Param("city") String city,
            @Param("postalCode") String postalCode,
            @Param("houseNumber") String houseNumber
    );
}
