package com.my.pet.project.mybank.accounts.controller;

import com.my.pet.project.mybank.accounts.dto.AccountCreateRequest;
import com.my.pet.project.mybank.accounts.dto.AccountUpdateRequest;
import com.my.pet.project.mybank.accounts.dto.BalanceUpdateRequest;
import com.my.pet.project.mybank.accounts.model.Account;
import com.my.pet.project.mybank.accounts.repository.AccountRepository;
import com.my.pet.project.mybank.accounts.service.OutboxPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.cloud.consul.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.cloud.loadbalancer.enabled=false",
        "spring.cloud.compatibility-verifier.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:9999/not-used",
        "spring.main.allow-bean-definition-overriding=true",
        "spring.autoconfigure.exclude=" +
                "org.springframework.cloud.autoconfigure.LifecycleMvcEndpointAutoConfiguration," +
                "org.springframework.cloud.autoconfigure.RefreshAutoConfiguration," +
                "org.springframework.cloud.autoconfigure.ConfigurationPropertiesRebinderAutoConfiguration," +
                "org.springframework.cloud.client.discovery.simple.SimpleDiscoveryClientAutoConfiguration," +
                "org.springframework.cloud.client.discovery.composite.CompositeDiscoveryClientAutoConfiguration"
})
@AutoConfigureMockMvc
@Testcontainers
class AccountControllerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @MockitoBean
    private OutboxPublisher outboxPublisher;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @TestConfiguration(proxyBeanMethods = false)
    static class TestConfig {

        @Bean
        ClientRegistrationRepository clientRegistrationRepository() {
            ClientRegistration registration = ClientRegistration.withRegistrationId("accounts-service")
                    .clientId("test-client")
                    .clientSecret("test-secret")
                    .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                    .tokenUri("http://localhost:9999/token")
                    .build();
            return new InMemoryClientRegistrationRepository(registration);
        }

        @Bean
        RestClient notificationsRestClient() {
            return RestClient.builder().baseUrl("http://localhost:9999").build();
        }
    }

    @BeforeEach
    void setUp() {
        accountRepository.deleteAll();
    }

    @Test
    void createAccount_returns201() throws Exception {
        AccountCreateRequest request = new AccountCreateRequest("user1", "John", "Doe", LocalDate.of(1990, 1, 1));

        mockMvc.perform(post("/accounts")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.login", is("user1")))
                .andExpect(jsonPath("$.firstName", is("John")))
                .andExpect(jsonPath("$.lastName", is("Doe")))
                .andExpect(jsonPath("$.balance", is(0)));
    }

    @Test
    void getAccountById_returns200() throws Exception {
        Account saved = accountRepository.save(
                new Account(null, "user2", "Jane", "Smith", LocalDate.of(1985, 5, 15), BigDecimal.ZERO));

        mockMvc.perform(get("/accounts/{id}", saved.getId())
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(saved.getId().intValue())))
                .andExpect(jsonPath("$.login", is("user2")))
                .andExpect(jsonPath("$.firstName", is("Jane")));
    }

    @Test
    void getAccountById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/accounts/999")
                        .with(jwt()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAccountByLogin_returns200() throws Exception {
        accountRepository.save(
                new Account(null, "user3", "Alice", "Brown", LocalDate.of(1992, 3, 20), BigDecimal.ZERO));

        mockMvc.perform(get("/accounts/login/user3")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login", is("user3")))
                .andExpect(jsonPath("$.firstName", is("Alice")));
    }

    @Test
    void getAllAccounts_returns200() throws Exception {
        accountRepository.save(
                new Account(null, "userA", "First", "One", LocalDate.of(1990, 1, 1), BigDecimal.ZERO));
        accountRepository.save(
                new Account(null, "userB", "Second", "Two", LocalDate.of(1991, 2, 2), BigDecimal.ZERO));

        mockMvc.perform(get("/accounts")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void updateAccount_returns200() throws Exception {
        Account saved = accountRepository.save(
                new Account(null, "user4", "OldFirst", "OldLast", LocalDate.of(1988, 7, 10), BigDecimal.ZERO));

        AccountUpdateRequest updateRequest = new AccountUpdateRequest("NewFirst", "NewLast", LocalDate.of(1989, 8, 11));

        mockMvc.perform(put("/accounts/{id}", saved.getId())
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName", is("NewFirst")))
                .andExpect(jsonPath("$.lastName", is("NewLast")));
    }

    @Test
    void updateBalance_returns200() throws Exception {
        Account saved = accountRepository.save(
                new Account(null, "user5", "Bob", "Builder", LocalDate.of(1983, 4, 5), new BigDecimal("100")));

        BalanceUpdateRequest balanceRequest = new BalanceUpdateRequest(new BigDecimal("200"));

        mockMvc.perform(patch("/accounts/{id}/balance", saved.getId())
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(balanceRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance", is(200)));
    }

    @Test
    void createDuplicateLogin_returns409() throws Exception {
        AccountCreateRequest request = new AccountCreateRequest("dupuser", "Test", "User", LocalDate.of(1990, 1, 1));

        mockMvc.perform(post("/accounts")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/accounts")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }
}
