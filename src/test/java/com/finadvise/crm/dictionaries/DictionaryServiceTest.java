package com.finadvise.crm.dictionaries;

import com.finadvise.crm.budget.BudgetReadFacade;
import com.finadvise.crm.clients.ClientReadFacade;
import com.finadvise.crm.products.ProductReadFacade;
import com.finadvise.crm.users.UserReadFacade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DictionaryServiceTest {

    @Mock private BudgetReadFacade budgetReadFacade;
    @Mock private ClientReadFacade clientReadFacade;
    @Mock private UserReadFacade userReadFacade;
    @Mock private ProductReadFacade productReadFacade;
    @Mock private DictionaryMapper dictionaryMapper;

    @InjectMocks
    private DictionaryService dictionaryService;

    // --- 1. DYNAMIC DICTIONARIES (ROUTING) ---

    @Test
    void getDynamicDictionaryItems_IncomeType_DelegatesToBudgetFacade() {
        List<DynamicDictionaryItemDTO> mockResult = List.of(new DynamicDictionaryItemDTO(1L, "Salary"));
        when(budgetReadFacade.getAllIncomeTypes()).thenReturn(mockResult);

        List<DynamicDictionaryItemDTO> result = dictionaryService.getDynamicDictionaryItems(DynamicDictionaryType.INCOME_TYPE);

        assertEquals(mockResult, result);
        verify(budgetReadFacade).getAllIncomeTypes();
        verifyNoInteractions(clientReadFacade, userReadFacade, productReadFacade, dictionaryMapper);
    }

    @Test
    void getDynamicDictionaryItems_ExpenseType_DelegatesToBudgetFacade() {
        List<DynamicDictionaryItemDTO> mockResult = List.of(new DynamicDictionaryItemDTO(2L, "Rent"));
        when(budgetReadFacade.getAllExpenseTypes()).thenReturn(mockResult);

        List<DynamicDictionaryItemDTO> result = dictionaryService.getDynamicDictionaryItems(DynamicDictionaryType.EXPENSE_TYPE);

        assertEquals(mockResult, result);
        verify(budgetReadFacade).getAllExpenseTypes();
        verifyNoInteractions(clientReadFacade, userReadFacade, productReadFacade, dictionaryMapper);
    }

    @Test
    void getDynamicDictionaryItems_ProductType_DelegatesToProductFacade() {
        List<DynamicDictionaryItemDTO> mockResult = List.of(new DynamicDictionaryItemDTO(3L, "Life Insurance"));
        when(productReadFacade.getAllProductTypes()).thenReturn(mockResult);

        List<DynamicDictionaryItemDTO> result = dictionaryService.getDynamicDictionaryItems(DynamicDictionaryType.PRODUCT_TYPE);

        assertEquals(mockResult, result);
        verify(productReadFacade).getAllProductTypes();
        verifyNoInteractions(clientReadFacade, userReadFacade, budgetReadFacade, dictionaryMapper);
    }

    @Test
    void getDynamicDictionaryItems_ProductProvider_DelegatesToProductFacade() {
        List<DynamicDictionaryItemDTO> mockResult = List.of(new DynamicDictionaryItemDTO(4L, "Acme Corp"));
        when(productReadFacade.getAllProductProviders()).thenReturn(mockResult);

        List<DynamicDictionaryItemDTO> result = dictionaryService.getDynamicDictionaryItems(DynamicDictionaryType.PRODUCT_PROVIDER);

        assertEquals(mockResult, result);
        verify(productReadFacade).getAllProductProviders();
        verifyNoInteractions(clientReadFacade, userReadFacade, budgetReadFacade, dictionaryMapper);
    }

    // --- 2. STATIC DICTIONARIES (ROUTING & MAPPING) ---

    @Test
    void getStaticDictionaryItems_ClientStatus_DelegatesToClientFacade() {
        List<StaticDictionaryItemDTO> mockResult = List.of(new StaticDictionaryItemDTO("ACTIVE", "Aktivní"));
        when(clientReadFacade.getAllClientStates()).thenReturn(mockResult);

        List<StaticDictionaryItemDTO> result = dictionaryService.getStaticDictionaryItems(StaticDictionaryType.CLIENT_STATUS);

        assertEquals(mockResult, result);
        verify(clientReadFacade).getAllClientStates();
        verifyNoInteractions(budgetReadFacade, userReadFacade, productReadFacade, dictionaryMapper);
    }

    @Test
    void getStaticDictionaryItems_ProductStatus_DelegatesToProductFacade() {
        List<StaticDictionaryItemDTO> mockResult = List.of(new StaticDictionaryItemDTO("ACTIVE", "Aktivní"));
        when(productReadFacade.getAllProductStates()).thenReturn(mockResult);

        List<StaticDictionaryItemDTO> result = dictionaryService.getStaticDictionaryItems(StaticDictionaryType.PRODUCT_STATUS);

        assertEquals(mockResult, result);
        verify(productReadFacade).getAllProductStates();
        verifyNoInteractions(budgetReadFacade, userReadFacade, clientReadFacade, dictionaryMapper);
    }

    @Test
    void getStaticDictionaryItems_UserStatus_DelegatesToUserFacade() {
        List<StaticDictionaryItemDTO> mockResult = List.of(new StaticDictionaryItemDTO("ACTIVE", "Aktivní"));
        when(userReadFacade.getAllUserStates()).thenReturn(mockResult);

        List<StaticDictionaryItemDTO> result = dictionaryService.getStaticDictionaryItems(StaticDictionaryType.USER_STATUS);

        assertEquals(mockResult, result);
        verify(userReadFacade).getAllUserStates();
        verifyNoInteractions(budgetReadFacade, productReadFacade, clientReadFacade, dictionaryMapper);
    }

    @Test
    void getStaticDictionaryItems_UserType_DelegatesToUserFacade() {
        List<StaticDictionaryItemDTO> mockResult = List.of(new StaticDictionaryItemDTO("ADVISOR", "Poradce"));
        when(userReadFacade.getAllUserTypes()).thenReturn(mockResult);

        List<StaticDictionaryItemDTO> result = dictionaryService.getStaticDictionaryItems(StaticDictionaryType.USER_TYPE);

        assertEquals(mockResult, result);
        verify(userReadFacade).getAllUserTypes();
        verifyNoInteractions(budgetReadFacade, productReadFacade, clientReadFacade, dictionaryMapper);
    }

    @Test
    void getStaticDictionaryItems_DynamicDictionariesIndex_MapsExistingTypes() {
        StaticDictionaryItemDTO mockMappedDto = new StaticDictionaryItemDTO("mock-path", "Mock Label");

        when(dictionaryMapper.toStaticDictionaryItemDto(any(DynamicDictionaryType.class))).thenReturn(mockMappedDto);

        List<StaticDictionaryItemDTO> result = dictionaryService.getStaticDictionaryItems(StaticDictionaryType.DYNAMIC_DICTIONARIES);

        assertNotNull(result);
        assertEquals(DynamicDictionaryType.values().length, result.size());

        // Ensure mapper was called exactly once for each dynamic dictionary type
        verify(dictionaryMapper, times(DynamicDictionaryType.values().length)).toStaticDictionaryItemDto(any(DynamicDictionaryType.class));

        verifyNoInteractions(budgetReadFacade, productReadFacade, clientReadFacade, userReadFacade);
    }
}