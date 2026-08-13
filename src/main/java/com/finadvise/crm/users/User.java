package com.finadvise.crm.users;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "USERS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements Persistable<Long> {

    @Id
    @Column(name = "USER_ID", nullable = false, unique = true)
    @NotNull(message = "user.id.required")
    private Long id;

    @Column(name = "EMPLOYEE_ID", nullable = false, unique = true, length = 20)
    @NotBlank(message = "user.employee-id.required")
    @Size(max = 20, message = "user.employee-id.size")
    private String employeeId;

    @Column(name = "PASSWORD_HASH", nullable = false, length = 255)
    @NotBlank(message = "user.password.required")
    @Size(max = 255, message = "user.password.size")
    private String passwordHash;

    @Column(name = "ICO", length = 8)
    @Pattern(regexp = "^\\d{8}$", message = "user.ico.format")
    private String ico;

    @Column(name = "FIRST_NAME", nullable = false, length = 50)
    @NotBlank(message = "user.first-name.required")
    @Size(max = 50, message = "user.first-name.size")
    private String firstName;

    @Column(name = "LAST_NAME", nullable = false, length = 50)
    @NotBlank(message = "user.last-name.required")
    @Size(max = 50, message = "user.last-name.size")
    private String lastName;

    @Column(name = "PHONE", nullable = false, length = 20)
    @NotBlank(message = "user.phone.required")
    @Size(max = 20, message = "user.phone.size")
    private String phone;

    @Column(name = "EMAIL", nullable = false, length = 254)
    @NotBlank(message = "user.email.required")
    @Size(max = 254, message = "user.email.size")
    @Email(message = "user.email.format")
    private String email;

    @Column(name = "USER_TYPE", nullable = false, length = 20)
    @NotNull(message = "user.type.required")
    @Enumerated(EnumType.STRING)
    private UserType userType;

    @Version
    @Column(name = "VERSION", nullable = false)
    private Integer version;

    @Column(name = "IS_ACTIVE", nullable = false)
    @JdbcTypeCode(SqlTypes.INTEGER)
    @Builder.Default
    private boolean isActive = true;

    @Transient // Tells Hibernate NOT to create a database column for this
    @Builder.Default
    private boolean isNewRecord = true;

    public String getRole() {
        return userType.name();
    }

    @Override
    public boolean isNew() {
        return isNewRecord;
    }

    @PostPersist
    @PostLoad
    protected void markNotNew() {
        this.isNewRecord = false;
    }
}