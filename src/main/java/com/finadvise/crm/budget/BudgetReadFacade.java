package com.finadvise.crm.budget;

import com.finadvise.crm.dictionaries.DynamicDictionaryItemDTO;

import java.util.List;

public interface BudgetReadFacade {
    FullBudgetDTO getFullBudgetForClient(String clientUid);
    List<DynamicDictionaryItemDTO> getAllIncomeTypes();
    List<DynamicDictionaryItemDTO> getAllExpenseTypes();
}
