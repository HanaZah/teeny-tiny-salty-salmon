package com.finadvise.crm.clients;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Immutable
@Table(name = "V_CLIENT_SEARCH_MINIMAL")
@Getter
public class ClientSearchMinimal {

    @Id // clientUid is guaranteed unique, so it safely acts as the JPA identifier
    @Column(name = "CLIENT_UID")
    private String clientUid;

    @Column(name = "ADVISOR_EMPLOYEE_ID")
    private String advisorEmployeeId;

    @Column(name = "PERSONAL_ID")
    private String personalId;

    @Column(name = "FULL_NAME")
    private String fullName;

    @Column(name = "CONTACT_CITY_NAME")
    private String contactCityName;

    @Column(name = "CONTACT_POSTAL_CODE")
    private String contactPostalCode;

    @Column(name = "IS_ACTIVE")
    @JdbcTypeCode(SqlTypes.INTEGER)
    private boolean isActive;
}