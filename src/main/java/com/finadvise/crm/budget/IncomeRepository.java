package com.finadvise.crm.budget;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

interface IncomeRepository extends JpaRepository<Income, Long> {

    @EntityGraph(attributePaths = {"incomeType"})
    @Query("SELECT i FROM Income i WHERE i.client.clientUid = :clientUid")
    List<Income> findAllByClientUidWithDetails(@Param("clientUid") String clientUid);

    List<Income> findAllByClient_ClientUid(String clientUid);
}
