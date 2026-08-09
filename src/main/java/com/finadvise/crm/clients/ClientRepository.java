package com.finadvise.crm.clients;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface ClientRepository extends JpaRepository<Client, Long> {
    Optional<Client> findByClientUidAndAdvisor_EmployeeId(String clientUid, String employeeId);
}
