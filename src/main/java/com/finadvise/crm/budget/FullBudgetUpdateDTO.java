package com.finadvise.crm.budget;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record FullBudgetUpdateDTO(
        @NotNull(message = "Incomes list is required")
        @Valid
        List<IncomeUpdateDTO> incomes,

        @NotNull(message = "Expenses list is required")
        @Valid
        List<ExpenseUpdateDTO> expenses
) {}