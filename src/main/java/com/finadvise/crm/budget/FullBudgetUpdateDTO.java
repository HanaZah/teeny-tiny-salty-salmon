package com.finadvise.crm.budget;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record FullBudgetUpdateDTO(
        @NotNull(message = "budget.incomes.required")
        @Valid
        List<IncomeUpdateDTO> incomes,

        @NotNull(message = "budget.expenses.required")
        @Valid
        List<ExpenseUpdateDTO> expenses
) {}