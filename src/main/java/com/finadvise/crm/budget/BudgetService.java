package com.finadvise.crm.budget;

import com.finadvise.crm.clients.Client;
import com.finadvise.crm.clients.ClientReadFacade;
import com.finadvise.crm.common.InvalidInputValueException;
import com.finadvise.crm.common.ResourceNotFoundException;
import com.finadvise.crm.dictionaries.DynamicDictionaryItemDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
class BudgetService implements BudgetReadFacade {
    private final IncomeRepository incomeRepository;
    private final ExpenseRepository expenseRepository;
    private final IncomeTypeRepository incomeTypeRepository;
    private final ExpenseTypeRepository expenseTypeRepository;
    private final BudgetMapper budgetMapper;
    private final ClientReadFacade clientReadFacade;

    @Override
    @Transactional(readOnly = true)
    public FullBudgetDTO getFullBudgetForClient(String clientUid) {
        return budgetMapper.toFullBudgetDto(
                incomeRepository.findAllByClientUidWithDetails(clientUid),
                expenseRepository.findAllByClientUidWithDetails(clientUid)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<DynamicDictionaryItemDTO> getAllIncomeTypes() {
        return incomeTypeRepository.findAll(Sort.by(Sort.Direction.ASC, IncomeType_.NAME))
                .stream().map(budgetMapper::toDynamicDictionaryItemDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DynamicDictionaryItemDTO> getAllExpenseTypes() {
        return expenseTypeRepository.findAll(Sort.by(Sort.Direction.ASC, ExpenseType_.NAME))
                .stream().map(budgetMapper::toDynamicDictionaryItemDto).toList();
    }

    @PreAuthorize("hasAuthority('ADVISOR')")
    @Transactional
    public FullBudgetDTO updateFullBudgetForClient(String clientUid, String employeeId, FullBudgetUpdateDTO dto) {
        long uniqueIncomeTypes = dto.incomes().stream().map(IncomeUpdateDTO::typeId).distinct().count();
        if (uniqueIncomeTypes != dto.incomes().size()) {
            throw new InvalidInputValueException("Unique income types constraint violated");
        }

        long uniqueExpenseTypes = dto.expenses().stream().map(ExpenseUpdateDTO::typeId).distinct().count();
        if (uniqueExpenseTypes != dto.expenses().size()) {
            throw new InvalidInputValueException("Unique expense types constraint violated");
        }

        Client client = clientReadFacade.findByClientUidAndAdvisorEmployeeId(clientUid, employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found or access denied"));
        client.validateEligibilityForUpdate();

        Map<Long, Income> currentIncomes = incomeRepository.findAllByClient_ClientUid(clientUid).stream()
                .collect(Collectors.toMap(i -> i.getIncomeType().getId(), i -> i));
        Map<Long, Expense> currentExpenses = expenseRepository.findAllByClient_ClientUid(clientUid).stream()
                .collect(Collectors.toMap(e -> e.getExpenseType().getId(), e -> e));

        List<Income> incomesToSave = new ArrayList<>();
        List<Income> incomesToDelete = new ArrayList<>();
        List<Expense> expensesToSave = new ArrayList<>();
        List<Expense> expensesToDelete = new ArrayList<>();


        for (IncomeUpdateDTO incomeDto : dto.incomes()) {
            Income existing = currentIncomes.get(incomeDto.typeId());

            if (existing != null) {
                if (incomeDto.amount() > 0 && !incomeDto.amount().equals(existing.getAmount())) {
                    existing.setAmount(incomeDto.amount());
                } else if (incomeDto.amount() == 0) {
                    incomesToDelete.add(existing);
                }
            } else if (incomeDto.amount() > 0) {
                Income newIncome = Income.builder()
                        .amount(incomeDto.amount())
                        .client(client)
                        .incomeType(incomeTypeRepository.getReferenceById(incomeDto.typeId()))
                        .build();
                incomesToSave.add(newIncome);
            }
        }

        for (ExpenseUpdateDTO expenseDto : dto.expenses()) {
            Expense existing = currentExpenses.get(expenseDto.typeId());

            if (existing != null) {
                if (expenseDto.amount() > 0
                    && (!expenseDto.amount().equals(existing.getAmount())
                        || !expenseDto.isMandatory().equals(existing.isMandatory()))) {

                    existing.setMandatory(expenseDto.isMandatory());
                    existing.setAmount(expenseDto.amount());
                } else if (expenseDto.amount() == 0) {
                    expensesToDelete.add(existing);
                }
            } else if (expenseDto.amount() > 0) {
                Expense newExpense = Expense.builder()
                        .amount(expenseDto.amount())
                        .client(client)
                        .expenseType(expenseTypeRepository.getReferenceById(expenseDto.typeId()))
                        .isMandatory(expenseDto.isMandatory())
                        .build();
                expensesToSave.add(newExpense);
            }
        }

        try {
            incomeRepository.saveAll(incomesToSave);
            incomeRepository.deleteAll(incomesToDelete);
            expenseRepository.saveAll(expensesToSave);
            expenseRepository.deleteAll(expensesToDelete);

            incomeRepository.flush();
            expenseRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new InvalidInputValueException("One or more provided budget item type IDs do not exist.");
        }

        return getFullBudgetForClient(clientUid);
    }
}
