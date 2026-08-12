package com.finadvise.crm.budget;

import com.finadvise.crm.dictionaries.DynamicDictionaryItemDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
class BudgetMapper {
    BudgetItemDTO toDto(Income income) {
        if (income == null) {
            return null;
        }

        return new BudgetItemDTO(
                income.getAmount(),
                income.getIncomeType().getId(),
                income.getIncomeType().getName(),
                null
        );
    }

    BudgetItemDTO toDto(Expense expense) {
        if (expense == null) {
            return null;
        }

        return new BudgetItemDTO(
                expense.getAmount(),
                expense.getExpenseType().getId(),
                expense.getExpenseType().getName(),
                expense.isMandatory()
        );
    }

    FullBudgetDTO toFullBudgetDto(List<Income> incomes, List<Expense> expenses) {
        return new FullBudgetDTO(
                incomes.stream().map(this::toDto).toList(),
                expenses.stream().map(this::toDto).toList(),
                incomes.stream().mapToInt(Income::getAmount).sum() - expenses.stream().mapToInt(Expense::getAmount).sum()
        );
    }

    DynamicDictionaryItemDTO toDynamicDictionaryItemDto(IncomeType type) {
        if (type == null) {
            return null;
        }
        return new DynamicDictionaryItemDTO(type.getId(), type.getName());
    }

    DynamicDictionaryItemDTO toDynamicDictionaryItemDto(ExpenseType type) {
        if (type == null) {
            return null;
        }
        return new DynamicDictionaryItemDTO(type.getId(), type.getName());
    }

}
