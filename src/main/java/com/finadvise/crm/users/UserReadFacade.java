package com.finadvise.crm.users;

import java.util.Optional;

/**
 * Safe read-only operations exposed to other packages.
 */
public interface UserReadFacade {
    Optional<User> findByEmployeeId(String employeeId);

}