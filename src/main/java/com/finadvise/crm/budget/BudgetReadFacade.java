package com.finadvise.crm.budget;

public interface BudgetReadFacade {
    FullBudgetDTO getFullBudgetForClient(String clientUid);
}
