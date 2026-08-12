package com.finadvise.crm.users;

import com.finadvise.crm.dictionaries.StaticDictionaryItemDTO;

import java.util.List;
import java.util.Optional;

/**
 * Safe read-only operations exposed to other packages.
 */
public interface UserReadFacade {
    Optional<User> findByEmployeeId(String employeeId);
    AdvisorSummaryDTO mapToAdvisorSummary(User user);
    List<StaticDictionaryItemDTO> getAllUserTypes();
    List<StaticDictionaryItemDTO> getAllUserStates();
}