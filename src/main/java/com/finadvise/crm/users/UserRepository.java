package com.finadvise.crm.users;

import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmployeeId(@NonNull String username);

    boolean existsByIco(String ico);

    boolean existsByEmployeeId(String employeeId);

    Optional<User> findFirstByUserTypeAndIsActiveIsTrue(UserType userType);

    default Optional<User> findFirstActiveByUserType_Admin() {
        return findFirstByUserTypeAndIsActiveIsTrue(UserType.ADMIN);
    }

    @Query(value = """
    SELECT
        (SELECT COUNT(*) FROM CLIENTS WHERE ADVISOR_ID = :userId AND IS_ACTIVE = 1) AS activeClients,
        (SELECT COUNT(*) FROM PRODUCTS WHERE ADVISOR_ID = :userId AND (END_DATE IS NULL OR END_DATE >= TRUNC(SYSDATE))) AS activeProducts
    FROM DUAL
    """, nativeQuery = true)
    AdvisorStatisticsProjection getAdvisorStatistics(@Param("userId") Long userId);

    @Query("""
        SELECT COUNT(u)
        FROM User u
        WHERE u.userType = :userType
          AND u.isActive = false
          AND (
              (SELECT COUNT(c) FROM Client c WHERE c.advisor = u AND c.isActive = true) > 0
              OR
              (SELECT COUNT(p) FROM Product p WHERE p.advisor = u AND p.endDate IS NOT NULL AND p.endDate > :referenceDate) > 0
          )
    """)
    long countOrphanedPortfolios(
            @Param("userType") UserType userType,
            @Param("referenceDate") LocalDate referenceDate
    );

    default long countOrphanedAdvisorPortfolios(LocalDate referenceDate) {
        return countOrphanedPortfolios(UserType.ADVISOR, referenceDate);
    }

    @Query(value = "SELECT USER_SEQ.NEXTVAL FROM dual", nativeQuery = true)
    Long getNextSequenceValue();

    @Query("""
        SELECT new com.finadvise.crm.users.AdvisorSuggestionResultDTO(
            u.employeeId,
            u.firstName || ' ' || u.lastName
        )
        FROM User u
        WHERE u.userType = :userType
          AND LOWER(u.firstName) || ' ' || LOWER(u.lastName) LIKE LOWER(CONCAT('%', :name, '%'))
    """)
    List<AdvisorSuggestionResultDTO> findUserSuggestions(
            @Param("name") String name,
            @Param("userType") UserType userType,
            Limit limit
    );

    default List<AdvisorSuggestionResultDTO> findAdvisorSuggestions(String name, int limit) {
        // Fallback to empty string if name is null to prevent NPE in the LIKE clause
        String safeName = name != null ? name : "";
        return findUserSuggestions(safeName, UserType.ADVISOR, Limit.of(limit));
    }

    @Modifying(clearAutomatically = true)
    @Query("UPDATE User u SET u.isActive = :isActive WHERE u.employeeId = :employeeId")
    void forceUpdateStatus(@Param("employeeId") String employeeId, @Param("isActive") boolean isActive);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE User u SET u.passwordHash = :passwordHash WHERE u.employeeId = :employeeId")
    void forceUpdatePassword(@Param("employeeId") String employeeId, @Param("passwordHash") String passwordHash);

}
