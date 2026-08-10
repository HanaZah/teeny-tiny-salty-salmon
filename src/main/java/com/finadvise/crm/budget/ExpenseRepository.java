package com.finadvise.crm.budget;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

interface ExpenseRepository extends JpaRepository<Expense, Long> {

    @EntityGraph(attributePaths = {"expenseType"})
    @Query("SELECT e FROM Expense e WHERE e.client.clientUid = :clientUid")
    List<Expense> findAllByClientUidWithDetails(@Param("clientUid") String clientUid);
    List<Expense> findAllByClient_ClientUid(String clientUid);
}
