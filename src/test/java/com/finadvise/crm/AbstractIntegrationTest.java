package com.finadvise.crm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.oracle.OracleContainer;


@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {
    @Autowired
    protected JdbcTemplate jdbcTemplate;

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

    /**
     * Executes a synchronous, native SQL wipe of all tables in strict reverse-dependency order.
     * Call this inside your test setup blocks to guarantee a clean state and avoid FK violations.
     */
    protected void cleanDatabase() {
        jdbcTemplate.execute("DELETE FROM PRODUCTS");
        jdbcTemplate.execute("DELETE FROM EXPENSES");
        jdbcTemplate.execute("DELETE FROM INCOMES");
        jdbcTemplate.execute("DELETE FROM CLIENTS");
        jdbcTemplate.execute("DELETE FROM USERS");
        jdbcTemplate.execute("DELETE FROM ADDRESSES");
        jdbcTemplate.execute("DELETE FROM PROVIDERS");
        jdbcTemplate.execute("DELETE FROM PRODUCT_TYPES");
        jdbcTemplate.execute("DELETE FROM EXPENSE_TYPES");
        jdbcTemplate.execute("DELETE FROM INCOME_TYPES");
    }
}