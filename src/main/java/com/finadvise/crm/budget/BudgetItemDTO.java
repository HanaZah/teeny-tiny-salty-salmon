package com.finadvise.crm.budget;

public record BudgetItemDTO(
        Integer amount,
        Long typeId,
        String typeName,
        Boolean isMandatory
) {}
