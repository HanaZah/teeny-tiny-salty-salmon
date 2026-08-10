package com.finadvise.crm.budget;

import org.springframework.data.jpa.repository.JpaRepository;

interface ExpenseTypeRepository extends JpaRepository<ExpenseType, Long> {
}
