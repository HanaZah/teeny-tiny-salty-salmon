package com.finadvise.crm.budget;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record IncomeUpdateDTO(
        @NotNull(message = "budget.income.amount.required")
        @Min(value = 0, message = "budget.income.amount.min-zero")
        @Max(value = 999999999, message = "budget.income.amount.max")
        Integer amount,

        @NotNull(message = "budget.income.type.required")
        Long typeId
) {}