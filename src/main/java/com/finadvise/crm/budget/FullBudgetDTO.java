package com.finadvise.crm.budget;

import java.util.List;

public record FullBudgetDTO(
        List<BudgetItemDTO> incomes,
        List<BudgetItemDTO> expenses,
        Integer totalCashFlow
) {}