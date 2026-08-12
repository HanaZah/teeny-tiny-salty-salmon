package com.finadvise.crm.dictionaries;

import com.finadvise.crm.budget.BudgetReadFacade;
import com.finadvise.crm.clients.ClientReadFacade;
import com.finadvise.crm.products.ProductReadFacade;
import com.finadvise.crm.users.UserReadFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
class DictionaryService {
    private final BudgetReadFacade budgetReadFacade;
    private final ClientReadFacade clientReadFacade;
    private final UserReadFacade userReadFacade;
    private final ProductReadFacade productReadFacade;
    private final DictionaryMapper dictionaryMapper;

    @Transactional(readOnly = true)
    public List<DynamicDictionaryItemDTO> getDynamicDictionaryItems(DynamicDictionaryType type) {
        return switch (type) {
            case INCOME_TYPE -> budgetReadFacade.getAllIncomeTypes();
            case EXPENSE_TYPE -> budgetReadFacade.getAllExpenseTypes();
            case PRODUCT_TYPE -> productReadFacade.getAllProductTypes();
            case PRODUCT_PROVIDER -> productReadFacade.getAllProductProviders();
        };
    }

    public List<StaticDictionaryItemDTO> getStaticDictionaryItems(StaticDictionaryType type) {
        return switch (type) {
            case CLIENT_STATUS -> clientReadFacade.getAllClientStates();
            case PRODUCT_STATUS -> productReadFacade.getAllProductStates();
            case USER_STATUS -> userReadFacade.getAllUserStates();
            case USER_TYPE -> userReadFacade.getAllUserTypes();
            case DYNAMIC_DICTIONARIES -> Arrays.stream(DynamicDictionaryType.values())
                                                .map(dictionaryMapper::toStaticDictionaryItemDto).toList();

        };
    }
}
