package com.finadvise.crm.budget;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "INCOME_TYPES")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class IncomeType {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "income_type_gen")
    @SequenceGenerator(name = "income_type_gen", sequenceName = "INCOME_TYPE_SEQ", allocationSize = 1)
    @Column(name = "INCOME_TYPE_ID")
    private Long id;

    @Column(name = "NAME", nullable = false, unique = true, length = 50)
    @NotBlank(message = "budget.income-type.name.required")
    @Size(max = 50, message = "budget.income-type.name.size")
    private String name;
}