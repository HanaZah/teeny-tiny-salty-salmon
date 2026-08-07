package com.finadvise.crm.users;

import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmployeeId(@NonNull String username);

    @Query(value = "SELECT USER_SEQ.NEXTVAL FROM dual", nativeQuery = true)
    Long getNextSequenceValue();

    boolean existsByEmail(String email);

    @Query("SELECT u.email  FROM User u WHERE u.employeeId = :employeeId")
    Optional<String> getEmailByEmployeeId(@Param("employeeId") String employeeId);
}
