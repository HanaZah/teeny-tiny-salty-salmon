package com.finadvise.crm.products;

import java.math.BigDecimal;

public record ProductsStatisticsDTO(
        Long total,
        Long active,
        Long activeManagedByRequester,
        BigDecimal totalMonthlyPayment
) {
    public ProductsStatisticsDTO {
        if (totalMonthlyPayment == null) {
            totalMonthlyPayment = BigDecimal.ZERO;
        }
    }
}