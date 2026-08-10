package com.finadvise.crm;

import com.finadvise.crm.addresses.Address;
import com.finadvise.crm.budget.Expense;
import com.finadvise.crm.budget.ExpenseType;
import com.finadvise.crm.budget.Income;
import com.finadvise.crm.budget.IncomeType;
import com.finadvise.crm.clients.Client;
import com.finadvise.crm.products.Product;
import com.finadvise.crm.products.ProductType;
import com.finadvise.crm.products.Provider;
import com.finadvise.crm.users.User;
import com.finadvise.crm.users.UserType;

import java.math.BigDecimal;
import java.time.LocalDate;

public class TestFixtureFactory {
    public static User createValidUser(String employeeId, UserType userType) {
        return User.builder()
                .id(1L)
                .ico("00000001")
                .employeeId(employeeId)
                .passwordHash("$2a$10$dXJ3ADWBr8t9BqbaEcKXvO7fH7Fm7ZtZ7yq7x7y7x7y7x7y7x7y7x") // Mock BCrypt string
                .firstName("John")
                .lastName("Doe")
                .email(employeeId + "@finadvise.com")
                .phone("+420123456789")
                .userType(userType)
                .version(0)
                .isActive(true)
                .build();
    }

    public static User createIntegrationUser(
            Long id, String employeeId, String encodedPassword, UserType userType) {
        return User.builder()
                .id(id)
                .ico(String.format("1%07d", id % 100000000))
                .employeeId(employeeId)
                .passwordHash(encodedPassword)
                .firstName("Integration")
                .lastName("Test")
                .email(employeeId + "@finadvise.com")
                .phone("+420111222333")
                .userType(userType)
                .isActive(true)
                .build();
    }

    public static Client createValidClient(Long id, String clientUid, User advisor) {
        Address dummyAddress = Address.builder()
                .id(1L)
                .street("Test Street")
                .houseNumber("123/A")
                .city("Test City")
                .postalCode("123 45")
                .build();

        return Client.builder()
                .id(id)
                .clientUid(clientUid)
                .personalId("900101" + String.format("%04d", id % 10000))
                .birthDate(LocalDate.of(1990, 1, 1))
                .firstName("John")
                .lastName("Smith")
                .occupation("Software Engineer")
                .phone("+420111222333")
                .email("client" + id + "@example.com")
                .idCardNumber(String.format("%09d", id % 1000000000))
                .idCardIssueDate(LocalDate.of(2020, 1, 1))
                .idCardExpiryDate(LocalDate.of(2030, 1, 1))
                .idCardIssuer("MV CR")
                .lastUpdate(LocalDate.now())
                .version(0)
                .isActive(true)
                .advisor(advisor)
                .residentialAddress(dummyAddress)
                .contactAddress(dummyAddress)
                .build();
    }

    public static Product createValidProduct(Long id, Client client, User advisor) {
        ProductType dummyType = ProductType.builder().id(1L).name("DummyProductType").build();
        Provider dummyProvider = Provider.builder().id(1L).name("DummyProductProvider").build();

        return Product.builder()
                .id(id)
                .name("Premium Life Coverage")
                .amount(new BigDecimal("5000.00"))
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(null)
                .productType(dummyType)
                .provider(dummyProvider)
                .client(client)
                .advisor(advisor)
                .build();
    }

    public static Address createIntegrationAddress(int uniqueHouseNumber) {
        return Address.builder()
                .street("Integration Street")
                .houseNumber(String.valueOf(uniqueHouseNumber % 9999 + 1))
                .city("Integration City")
                .postalCode("123 45")
                .build();
    }

    public static Client createIntegrationClient(Long id, String clientUid, User advisor, Address address) {
        return Client.builder()
                .id(id)
                .clientUid(clientUid)
                .personalId(String.format("900101%04d", id % 10000))
                .birthDate(LocalDate.of(1990, 1, 1))
                .firstName("Integration")
                .lastName("Client")
                .occupation("Test Subject")
                .phone("+420999888777")
                .email("int.client" + id + "@finadvise.com")
                .idCardNumber(String.format("1%08d", id % 100000000))
                .idCardIssueDate(LocalDate.of(2020, 1, 1))
                .idCardExpiryDate(LocalDate.now().plusYears(1)) // Dynamically adjusting to avoid time-bomb tests
                .idCardIssuer("MV CR")
                .lastUpdate(LocalDate.now())
                .version(0)
                .isActive(true)
                .advisor(advisor)
                .residentialAddress(address)
                .contactAddress(address)
                .build();
    }

    public static User createIntegrationAdmin(Long id, String employeeId, String encodedPassword) {
        return createIntegrationUser(id, employeeId, encodedPassword, UserType.ADMIN);
    }

    public static Income createValidIncome(Long typeId, String typeName, Integer amount, Client client) {
        IncomeType type = IncomeType.builder()
                .id(typeId)
                .name(typeName)
                .build();

        return Income.builder()
                .id(typeId * 100) // Arbitrary mock ID
                .amount(amount)
                .incomeType(type)
                .client(client)
                .build();
    }

    public static Expense createValidExpense(Long typeId, String typeName, Integer amount, boolean isMandatory, Client client) {
        ExpenseType type = ExpenseType.builder()
                .id(typeId)
                .name(typeName)
                .build();

        return Expense.builder()
                .id(typeId * 100) // Arbitrary mock ID
                .amount(amount)
                .isMandatory(isMandatory)
                .expenseType(type)
                .client(client)
                .build();
    }

    public static Income createIntegrationIncome(Client client, IncomeType type, Integer amount) {
        return Income.builder()
                // ID intentionally omitted so @GeneratedValue can work natively
                .amount(amount)
                .incomeType(type)
                .client(client)
                .build();
    }

    public static Expense createIntegrationExpense(Client client, ExpenseType type, Integer amount, boolean isMandatory) {
        return Expense.builder()
                // ID intentionally omitted so @GeneratedValue can work natively
                .amount(amount)
                .isMandatory(isMandatory)
                .expenseType(type)
                .client(client)
                .build();
    }
}