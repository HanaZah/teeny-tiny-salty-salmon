package com.finadvise.crm.budget;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ExpenseUpdateDTO(
        @NotNull(message = "Amount is required")
        @Min(value = 0, message = "Amount must be greater than or equal to 0")
        @Max(value = 999999999, message = "Amount must be less than or equal to 999,999,999")
        Integer amount,

        @NotNull(message = "Type ID is required")
        Long typeId,

        @NotNull(message = "Mandatory expense flag is required")
        Boolean isMandatory
) {}