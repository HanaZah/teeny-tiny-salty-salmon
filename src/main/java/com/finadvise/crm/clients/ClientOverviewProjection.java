package com.finadvise.crm.clients;

public interface ClientOverviewProjection {
    String getClientUid();
    String getFirstName();
    String getLastName();
    String getOccupation();
    Long getActiveProducts();
    Long getTotalIncome();
    Long getTotalExpense();
}