package com.finadvise.crm.addresses;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Entity
@Table(name = "ADDRESSES")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "addr_gen")
    @SequenceGenerator(name = "addr_gen", sequenceName = "ADDR_SEQ", allocationSize = 1)
    @Column(name = "ADDRESS_ID", updatable = false)
    private Long id;

    @Column(name="POSTAL_CODE", nullable = false, length = 6)
    @NotBlank(message = "address.postal-code.required")
    @Pattern(regexp = "^\\d{3}\\s\\d{2}$", message = "address.postal-code.format")
    private String postalCode;

    @Column(name = "CITY", nullable = false, length = 100)
    @NotBlank(message = "address.city.required")
    private String city;

    @Column(name = "STREET", nullable = false, length = 100)
    @NotBlank(message = "address.street.required")
    private String street;

    @Column(name = "HOUSE_NUMBER", nullable = false, length = 10)
    @NotBlank(message = "address.house-number.required")
    @Pattern(
            regexp = "^[1-9]\\d{0,3}(/[1-9]\\d{0,3}[a-z]?)?$",
            message = "address.house-number.format"
    )
    private String houseNumber;

    public boolean matches(String targetStreet, String targetHouseNumber, String targetCity, String targetPostalCode) {
        return this.houseNumber.equalsIgnoreCase(targetHouseNumber) &&
                this.street.equalsIgnoreCase(targetStreet) &&
                this.city.equalsIgnoreCase(targetCity) &&
                this.postalCode.equalsIgnoreCase(targetPostalCode);
    }
}