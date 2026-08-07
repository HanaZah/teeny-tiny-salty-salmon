package com.finadvise.crm;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.oracle.OracleContainer;

@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    static final OracleContainer ORACLE_CONTAINER;

    // The static block ensures the container starts exactly once per JVM run,
    // avoiding the context caching clashes between different test classes.
    static {
        ORACLE_CONTAINER = new OracleContainer("gvenzl/oracle-free:slim-faststart");
        ORACLE_CONTAINER.start();
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", ORACLE_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", ORACLE_CONTAINER::getUsername);
        registry.add("spring.datasource.password", ORACLE_CONTAINER::getPassword);
    }
}