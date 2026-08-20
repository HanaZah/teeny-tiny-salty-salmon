package com.finadvise.crm.budget;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(30)
public class BudgetDatabaseSeeder implements CommandLineRunner {
    private final IncomeTypeRepository incomeTypeRepository;
    private final ExpenseTypeRepository expenseTypeRepository;

    @Override
    public void run(String @NonNull ... args) {
        if (incomeTypeRepository.count() == 0) {
            incomeTypeRepository.saveAll(List.of(
                    IncomeType.builder().name("Zaměstnání").build(),
                    IncomeType.builder().name("Podnikání").build(),
                    IncomeType.builder().name("Z nájmu").build(),
                    IncomeType.builder().name("Dávky a důchody").build(),
                    IncomeType.builder().name("Z investic").build(),
                    IncomeType.builder().name("Ostatní").build()
            ));
            log.info("Seeded initial Income Types.");
        }

        if (expenseTypeRepository.count() == 0) {
            expenseTypeRepository.saveAll(List.of(
                    ExpenseType.builder().name("Bydlení").build(),
                    ExpenseType.builder().name("Splátky").build(),
                    ExpenseType.builder().name("Pojištění").build(),
                    ExpenseType.builder().name("Potraviny").build(),
                    ExpenseType.builder().name("Drogerie").build(),
                    ExpenseType.builder().name("Oblečení a obuv").build(),
                    ExpenseType.builder().name("Doprava").build(),
                    ExpenseType.builder().name("Koníčky").build(),
                    ExpenseType.builder().name("Zábava").build(),
                    ExpenseType.builder().name("Děti").build(),
                    ExpenseType.builder().name("Mazlíčci").build(),
                    ExpenseType.builder().name("Léky").build(),
                    ExpenseType.builder().name("Spoření").build(),
                    ExpenseType.builder().name("Ostatní").build()
            ));
            log.info("Seeded initial Expense Types.");
        }
    }
}
