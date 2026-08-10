package com.finadvise.crm.budget;

import com.finadvise.crm.TestFixtureFactory;
import com.finadvise.crm.clients.Client;
import com.finadvise.crm.clients.ClientReadFacade;
import com.finadvise.crm.common.InvalidInputValueException;
import com.finadvise.crm.common.ResourceNotFoundException;
import com.finadvise.crm.users.User;
import com.finadvise.crm.users.UserType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock private IncomeRepository incomeRepository;
    @Mock private ExpenseRepository expenseRepository;
    @Mock private IncomeTypeRepository incomeTypeRepository;
    @Mock private ExpenseTypeRepository expenseTypeRepository; // mocked for injection even if not used in the tests
    @Mock private BudgetMapper budgetMapper;
    @Mock private ClientReadFacade clientReadFacade;

    @InjectMocks
    private BudgetService budgetService;

    @Captor private ArgumentCaptor<List<Income>> incomeListCaptor;

    private User mockAdvisor;
    private Client mockClient;

    @BeforeEach
    void setUp() {
        mockAdvisor = TestFixtureFactory.createValidUser("EMP-123", UserType.ADVISOR);
        mockClient = TestFixtureFactory.createValidClient(1L, "C-123456", mockAdvisor);
    }

    // --- GET FULL BUDGET ---

    @Test
    void getFullBudgetForClient_OrchestrationSuccess() {
        List<Income> mockIncomes = List.of(TestFixtureFactory.createValidIncome(1L, "Salary", 1000, mockClient));
        List<Expense> mockExpenses = List.of(TestFixtureFactory.createValidExpense(1L, "Rent", 500, true, mockClient));
        FullBudgetDTO expectedDto = new FullBudgetDTO(List.of(), List.of(), 500);

        when(incomeRepository.findAllByClientUidWithDetails(mockClient.getClientUid())).thenReturn(mockIncomes);
        when(expenseRepository.findAllByClientUidWithDetails(mockClient.getClientUid())).thenReturn(mockExpenses);
        when(budgetMapper.toFullBudgetDto(mockIncomes, mockExpenses)).thenReturn(expectedDto);

        FullBudgetDTO result = budgetService.getFullBudgetForClient(mockClient.getClientUid());

        assertNotNull(result);
        assertEquals(500, result.totalCashFlow());
        verify(budgetMapper).toFullBudgetDto(mockIncomes, mockExpenses);
    }

    // --- UPDATE FULL BUDGET ---

    @Test
    void updateFullBudgetForClient_Success_SortsBucketsCorrectly() {
        Income existingIncomeToUpdate = TestFixtureFactory.createValidIncome(1L, "Salary", 1000, mockClient);
        Income existingIncomeUnchanged = TestFixtureFactory.createValidIncome(2L, "Bonus", 500, mockClient);
        Income existingIncomeToDelete = TestFixtureFactory.createValidIncome(3L, "Gift", 200, mockClient);

        when(clientReadFacade.findByClientUidAndAdvisorEmployeeId(mockClient.getClientUid(), mockAdvisor.getEmployeeId()))
                .thenReturn(Optional.of(mockClient));
        when(incomeRepository.findAllByClient_ClientUid(mockClient.getClientUid()))
                .thenReturn(List.of(existingIncomeToUpdate, existingIncomeUnchanged, existingIncomeToDelete));
        when(expenseRepository.findAllByClient_ClientUid(mockClient.getClientUid()))
                .thenReturn(List.of()); // Keep expenses simple for this test

        FullBudgetUpdateDTO request = new FullBudgetUpdateDTO(
                List.of(
                        new IncomeUpdateDTO(1200, 1L), // Update (1000 -> 1200)
                        new IncomeUpdateDTO(500, 2L),  // Unchanged
                        new IncomeUpdateDTO(0, 3L),    // Delete
                        new IncomeUpdateDTO(800, 4L)   // Create (New)
                ),
                List.of()
        );

        IncomeType newType = IncomeType.builder().id(4L).name("New Type").build();
        when(incomeTypeRepository.getReferenceById(4L)).thenReturn(newType);

        budgetService.updateFullBudgetForClient(mockClient.getClientUid(), mockAdvisor.getEmployeeId(), request);

        // Verify Save operations (should contain only the new item, the updated item is handled via Hibernate dirty checking)
        verify(incomeRepository).saveAll(incomeListCaptor.capture());
        List<Income> savedIncomes = incomeListCaptor.getValue();
        assertEquals(1, savedIncomes.size());
        assertTrue(savedIncomes.stream().anyMatch(i -> i.getAmount() == 800 && i.getIncomeType().getId() == 4L));

        // Verify Delete operations (Should contain exactly the item marked with amount 0)
        verify(incomeRepository).deleteAll(incomeListCaptor.capture());
        List<Income> deletedIncomes = incomeListCaptor.getValue();
        assertEquals(1, deletedIncomes.size());
        assertEquals(3L, deletedIncomes.getFirst().getIncomeType().getId());
    }

    @Test
    void updateFullBudgetForClient_UniqueIncomeTypesConstraintViolation_ThrowsException() {
        FullBudgetUpdateDTO request = new FullBudgetUpdateDTO(
                List.of(
                        new IncomeUpdateDTO(1000, 1L),
                        new IncomeUpdateDTO(500, 1L) // Duplicate Type ID!
                ),
                List.of()
        );

        assertThrows(InvalidInputValueException.class, () ->
                        budgetService.updateFullBudgetForClient(mockClient.getClientUid(), mockAdvisor.getEmployeeId(), request),
                "Unique income types constraint violated"
        );
        verifyNoInteractions(clientReadFacade, incomeRepository, expenseRepository);
    }

    @Test
    void updateFullBudgetForClient_UniqueExpenseTypesConstraintViolation_ThrowsException() {
        FullBudgetUpdateDTO request = new FullBudgetUpdateDTO(
                List.of(),
                List.of(
                        new ExpenseUpdateDTO(1000, 1L, true),
                        new ExpenseUpdateDTO(500, 1L, false) // Duplicate Type ID!
                )
        );

        assertThrows(InvalidInputValueException.class, () ->
                        budgetService.updateFullBudgetForClient(mockClient.getClientUid(), mockAdvisor.getEmployeeId(), request),
                "Unique expense types constraint violated"
        );
        verifyNoInteractions(clientReadFacade, incomeRepository, expenseRepository);
    }

    @Test
    void updateFullBudgetForClient_NotFoundOrAccessDenied_ThrowsException() {
        FullBudgetUpdateDTO request = new FullBudgetUpdateDTO(List.of(), List.of());
        when(clientReadFacade.findByClientUidAndAdvisorEmployeeId("UNKNOWN", mockAdvisor.getEmployeeId()))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                budgetService.updateFullBudgetForClient("UNKNOWN", mockAdvisor.getEmployeeId(), request)
        );
    }

    @Test
    void updateFullBudgetForClient_DataIntegrityViolation_ThrowsInvalidInputValue() {
        FullBudgetUpdateDTO request = new FullBudgetUpdateDTO(
                List.of(new IncomeUpdateDTO(1000, 999L)), List.of() // 999L is invalid
        );

        when(clientReadFacade.findByClientUidAndAdvisorEmployeeId(mockClient.getClientUid(), mockAdvisor.getEmployeeId()))
                .thenReturn(Optional.of(mockClient));
        when(incomeTypeRepository.getReferenceById(999L)).thenReturn(IncomeType.builder().id(999L).build());

        doThrow(new DataIntegrityViolationException("Constraint violation"))
                .when(incomeRepository).flush();

        assertThrows(InvalidInputValueException.class, () ->
                budgetService.updateFullBudgetForClient(mockClient.getClientUid(), mockAdvisor.getEmployeeId(), request)
        );
    }
}