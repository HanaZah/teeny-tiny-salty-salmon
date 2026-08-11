package com.finadvise.crm.clients;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

interface ClientRepository extends JpaRepository<Client, Long>{
    Optional<Client> findByClientUidAndAdvisor_EmployeeId(String clientUid, String employeeId);

    @EntityGraph(attributePaths = {"advisor", "residentialAddress", "contactAddress"})
    @Query("SELECT c FROM Client c WHERE c.clientUid = :clientUid AND c.advisor.employeeId = :employeeId")
    Optional<Client> findByClientUidAndAdvisor_EmployeeIdWithDetails(
            @Param("clientUid") String clientUid,
            @Param("employeeId") String employeeId);

    @EntityGraph(attributePaths = {"advisor", "residentialAddress", "contactAddress"})
    @Query("SELECT c FROM Client c WHERE c.clientUid = :clientUid")
    Optional<Client> findByClientUidWithDetails(@Param("clientUid") String clientUid);

    @Query("""
        SELECT
            c.clientUid AS clientUid,
            c.firstName AS firstName,
            c.lastName AS lastName,
            c.occupation AS occupation,
            (SELECT COUNT(p) FROM Product p WHERE p.client = c AND p.startDate <= :currentDate AND (p.endDate IS NULL OR p.endDate >= :currentDate)) AS activeProducts,
            (SELECT COALESCE(SUM(i.amount), 0L) FROM Income i WHERE i.client = c) AS totalIncome,
            (SELECT COALESCE(SUM(e.amount), 0L) FROM Expense e WHERE e.client = c) AS totalExpense
        FROM Client c
        WHERE c.advisor.employeeId = :employeeId
            AND c.isActive = true
        ORDER BY c.lastUpdate DESC
    """)
    List<ClientOverviewProjection> findRecentClientOverviews(
            @Param("employeeId") String employeeId,
            @Param("currentDate") LocalDate currentDate,
            Limit limit
    );

    @Query("""
        SELECT new com.finadvise.crm.clients.ClientSuggestionResultDTO(
            c.clientUid,
            c.firstName || ' ' || c.lastName
        )
        FROM Client c
        WHERE LOWER(c.firstName) || ' ' || LOWER(c.lastName) LIKE LOWER(CONCAT('%', :name, '%'))
    """)
    List<ClientSuggestionResultDTO> findClientSuggestions(
            @Param("name") String name,
            Limit limit
    );

    default List<ClientSuggestionResultDTO> findClientSuggestions(String name, int limit) {
        String safeName = name != null ? name : "";
        return findClientSuggestions(safeName, Limit.of(limit));
    }

    @Query("""
        SELECT new com.finadvise.crm.clients.ClientSuggestionResultDTO(
            c.clientUid,
            c.firstName || ' ' || c.lastName
        )
        FROM Client c
        WHERE c.advisor.employeeId = :employeeId
            AND LOWER(c.firstName) || ' ' || LOWER(c.lastName) LIKE LOWER(CONCAT('%', :name, '%'))
    """)
    List<ClientSuggestionResultDTO> findClientSuggestionsForAdvisor(
            @Param("name") String name,
            @Param("employeeId") String employeeId,
            Limit limit
    );

    default List<ClientSuggestionResultDTO> findClientSuggestionsForAdvisor(String name, String employeeId, int limit) {
        String safeName = name != null ? name : "";
        return findClientSuggestionsForAdvisor(safeName, employeeId, Limit.of(limit));
    }

    boolean existsByIdCardNumber(String idCardNumber);

    boolean existsByPersonalId(String personalId);

    @Query(value = "SELECT CLIENT_SEQ.NEXTVAL FROM dual", nativeQuery = true)
    Long getNextSequenceValue();
}
