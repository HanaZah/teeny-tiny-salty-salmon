package com.finadvise.crm.clients;

import com.finadvise.crm.addresses.Address;
import com.finadvise.crm.common.InvalidInputValueException;
import com.finadvise.crm.users.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import java.time.LocalDate;

@Entity
@Table(name = "CLIENTS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Client implements Persistable<Long> {

    @Id
    @Column(name = "CLIENT_ID")
    private Long id;

    @Column(name = "CLIENT_UID", nullable = false, unique = true, length = 8)
    private String clientUid;

    @Column(name = "PERSONAL_ID", nullable = false, unique = true, length = 10)
    @NotBlank(message = "client.personal-id.required")
    @Size(min = 10, max = 10, message = "client.personal-id.size")
    @Pattern(regexp = "^\\d{10}$", message = "client.personal-id.format")
    private String personalId;

    @Column(name = "BIRTH_DATE", nullable = false)
    @NotNull(message = "client.birth-date.required")
    private LocalDate birthDate;

    @Column(name = "FIRST_NAME", nullable = false, length = 50)
    @NotBlank(message = "client.first-name.required")
    @Size(max = 50, message = "client.first-name.size")
    @Pattern(regexp = "^[\\p{L}\\p{M}\\d\\s\\-'.]+$", message = "client.first-name.format")
    private String firstName;

    @Column(name = "LAST_NAME", nullable = false, length = 50)
    @NotBlank(message = "client.last-name.required")
    @Size(max = 50, message = "client.last-name.size")
    @Pattern(regexp = "^[\\p{L}\\p{M}\\d\\s\\-'.]+$", message = "client.last-name.format")
    private String lastName;

    @Column(name = "OCCUPATION", nullable = false, length = 100)
    @NotBlank(message = "client.occupation.required")
    @Size(max = 100, message = "client.occupation.size")
    @Pattern(regexp = "^[\\p{L}\\p{M}\\d\\s\\-'.,]+$", message = "client.occupation.format")
    private String occupation;

    @Column(name = "PHONE", nullable = false, length = 20)
    @NotBlank(message = "client.phone.required")
    @Size(max = 20, message = "client.phone.size")
    @Pattern(regexp = "^\\+?[\\d\\s\\-]+$", message = "client.phone.format")
    private String phone;

    @Column(name = "EMAIL", nullable = false, length = 254)
    @NotBlank(message = "client.email.required")
    @Size(max = 254, message = "client.email.size")
    @Email(message = "client.email.format")
    private String email;

    @Column(name = "ID_CARD_NUMBER", nullable = false, unique = true)
    @NotBlank(message = "client.id-card-number.required")
    @Size(min = 9, max = 9, message = "client.id-card-number.size")
    @Pattern(regexp = "^\\d{9}$", message = "client.id-card-number.format")
    private String idCardNumber;

    @Column(name = "ID_CARD_ISSUE_DATE", nullable = false)
    @NotNull(message = "client.id-card-issue-date.required")
    private LocalDate idCardIssueDate;

    @Column(name = "ID_CARD_EXPIRY_DATE", nullable = false)
    @NotNull(message = "client.id-card-expiry-date.required")
    private LocalDate idCardExpiryDate;

    @Column(name = "ID_CARD_ISSUER", nullable = false, length = 100)
    @NotBlank(message = "client.id-card-issuer.required")
    @Size(max = 100, message = "client.id-card-issuer.size")
    @Pattern(regexp = "^[\\p{L}\\p{M}\\d\\s\\-'.]+$", message = "client.id-card-issuer.format")
    private String idCardIssuer;

    @Column(name = "LAST_UPDATE", nullable = false)
    private LocalDate lastUpdate;

    @Version
    @Column(name = "VERSION", nullable = false)
    @Builder.Default
    private Integer version = 0;

    @Column(name = "IS_ACTIVE", nullable = false)
    @JdbcTypeCode(SqlTypes.INTEGER)
    @Builder.Default
    private boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ADVISOR_ID", nullable = false)
    private User advisor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RESIDENT_ADDRESS_ID", nullable = false)
    private Address residentialAddress;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CONTACT_ADDRESS_ID", nullable = false)
    private Address contactAddress;

    @Transient
    @Builder.Default
    private boolean isNewRecord = true;

    @Override
    public boolean isNew() {
        return isNewRecord;
    }

    @PostPersist
    @PostLoad
    protected void markNotNew() {
        this.isNewRecord = false;
    }

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.lastUpdate = LocalDate.now();
    }

    public void validateEligibilityForNewProduct(LocalDate currentDate) {
        if (!this.isActive) {
            throw new InvalidInputValueException("error.client.product.inactive");
        }
        if (this.idCardExpiryDate.isBefore(currentDate)) {
            throw new InvalidInputValueException("error.client.product.id-card-expired");
        }
    }

    public void validateEligibilityForUpdate() {
        if (!this.isActive) {
            throw new InvalidInputValueException("error.client.update.inactive");
        }
    }
}