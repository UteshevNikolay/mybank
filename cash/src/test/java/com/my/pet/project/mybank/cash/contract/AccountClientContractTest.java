package com.my.pet.project.mybank.cash.contract;

import com.my.pet.project.mybank.cash.client.AccountClient;
import com.my.pet.project.mybank.cash.dto.AccountResponse;
import com.my.pet.project.mybank.cash.dto.BalanceUpdateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "stubrunner.ids=com.my.pet.project:accounts:+:stubs:6565",
                "stubrunner.stubsMode=LOCAL"
        }
)
@AutoConfigureStubRunner
@ActiveProfiles("test")
@Testcontainers
class AccountClientContractTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private AccountClient accountClient;

    @TestConfiguration
    static class StubConfig {
        @Bean
        public RestClient accountsRestClient() {
            return RestClient.builder()
                    .baseUrl("http://localhost:6565")
                    .build();
        }

        @Bean
        public RestClient notificationsRestClient() {
            return RestClient.builder()
                    .baseUrl("http://localhost:9999")
                    .build();
        }
    }

    @Test
    void shouldGetAccountById() {
        AccountResponse response = accountClient.getAccountById(1L);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.login()).isEqualTo("user1");
        assertThat(response.firstName()).isEqualTo("John");
        assertThat(response.lastName()).isEqualTo("Doe");
        assertThat(response.balance()).isEqualByComparingTo(new BigDecimal("500.00"));
    }

    @Test
    void shouldUpdateBalance() {
        BalanceUpdateRequest request = new BalanceUpdateRequest(new BigDecimal("600.00"));
        AccountResponse response = accountClient.updateBalance(1L, request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.balance()).isEqualByComparingTo(new BigDecimal("600.00"));
    }
}
