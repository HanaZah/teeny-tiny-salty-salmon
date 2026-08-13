package com.finadvise.crm.budget;

import com.finadvise.crm.clients.Client;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

@Entity
@Table(name = "INCOMES")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Income {

    @Id
    @Column(name = "INCOME_ID")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "budget_item_gen")
    @SequenceGenerator(name = "budget_item_gen", sequenceName = "BUDGET_ITEM_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "AMOUNT", nullable = false)
    @Min(value = 1, message = "budget.income.amount.min")
    @Max(value = 999999999, message = "budget.income.amount.max")
    @NotNull(message = "budget.income.amount.required")
    private Integer amount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CLIENT_ID", nullable = false, updatable = false)
    @NotNull(message = "budget.income.client.required")
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "INCOME_TYPE_ID", nullable = false)
    @Fetch(FetchMode.JOIN)
    @NotNull(message = "budget.income.type.required")
    private IncomeType incomeType;
}