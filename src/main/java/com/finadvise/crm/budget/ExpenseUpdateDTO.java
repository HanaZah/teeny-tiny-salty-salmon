package com.finadvise.crm.budget;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ExpenseUpdateDTO(
        @NotNull(message = "budget.expense.amount.required")
        @Min(value = 0, message = "budget.expense.amount.min-zero")
        @Max(value = 999999999, message = "budget.expense.amount.max")
        Integer amount,

        @NotNull(message = "budget.expense.type.required")
        Long typeId,

        @NotNull(message = "budget.expense.mandatory.required")
        Boolean isMandatory
) {}