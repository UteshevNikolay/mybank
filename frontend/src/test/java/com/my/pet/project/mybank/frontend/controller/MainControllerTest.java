package com.my.pet.project.mybank.frontend.controller;

import com.my.pet.project.mybank.frontend.client.AccountClient;
import com.my.pet.project.mybank.frontend.client.CashClient;
import com.my.pet.project.mybank.frontend.client.TransferClient;
import com.my.pet.project.mybank.frontend.config.SecurityConfig;
import com.my.pet.project.mybank.frontend.dto.AccountResponse;
import com.my.pet.project.mybank.frontend.dto.CashResponse;
import com.my.pet.project.mybank.frontend.dto.TransferResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(value = MainController.class)
@Import(SecurityConfig.class)
class MainControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountClient accountClient;

    @MockitoBean
    private CashClient cashClient;

    @MockitoBean
    private TransferClient transferClient;

    @TestConfiguration(proxyBeanMethods = false)
    static class TestConfig {

        @Bean
        ClientRegistrationRepository clientRegistrationRepository() {
            ClientRegistration registration = ClientRegistration.withRegistrationId("keycloak")
                    .clientId("test-client")
                    .clientSecret("test-secret")
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                    .authorizationUri("http://localhost:9999/auth")
                    .tokenUri("http://localhost:9999/token")
                    .userInfoUri("http://localhost:9999/userinfo")
                    .userNameAttributeName("preferred_username")
                    .build();
            return new InMemoryClientRegistrationRepository(registration);
        }
    }

    private OidcUser buildOidcUser(String preferredUsername) {
        Map<String, Object> claims = Map.of(
                "sub", "test-sub",
                "preferred_username", preferredUsername,
                "iss", "http://localhost:9999"
        );
        OidcIdToken idToken = new OidcIdToken(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                claims
        );
        return new DefaultOidcUser(List.of(), idToken, "preferred_username");
    }

    @Test
    void getAccount_returnsMainView() throws Exception {
        OidcUser oidcUser = buildOidcUser("testuser");
        AccountResponse account = new AccountResponse(1L, "testuser", "Ivan", "Petrov",
                LocalDate.of(1990, 1, 1), BigDecimal.valueOf(500), 0L);
        AccountResponse otherAccount = new AccountResponse(2L, "otheruser", "Maria", "Sidorova",
                LocalDate.of(1985, 5, 15), BigDecimal.valueOf(300), 0L);

        when(accountClient.getAccountByLogin("testuser")).thenReturn(account);
        when(accountClient.getAllAccounts()).thenReturn(List.of(account, otherAccount));

        mockMvc.perform(get("/account").with(oidcLogin().oidcUser(oidcUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("main"))
                .andExpect(model().attribute("name", "Petrov Ivan"))
                .andExpect(model().attribute("sum", 500));
    }

    @Test
    void editCash_returnsMainView() throws Exception {
        OidcUser oidcUser = buildOidcUser("testuser");
        AccountResponse account = new AccountResponse(1L, "testuser", "Ivan", "Petrov",
                LocalDate.of(1990, 1, 1), BigDecimal.valueOf(600), 0L);
        CashResponse cashResponse = new CashResponse("Deposit successful", BigDecimal.valueOf(600));

        when(accountClient.getAccountByLogin("testuser")).thenReturn(account);
        when(accountClient.getAllAccounts()).thenReturn(List.of(account));
        when(cashClient.processCash(1L, new BigDecimal("100"), "PUT")).thenReturn(cashResponse);

        mockMvc.perform(post("/cash")
                        .with(oidcLogin().oidcUser(oidcUser))
                        .with(csrf())
                        .param("value", "100")
                        .param("action", "PUT"))
                .andExpect(status().isOk())
                .andExpect(view().name("main"))
                .andExpect(model().attribute("info", "Deposit successful"));
    }

    @Test
    void transfer_returnsMainView() throws Exception {
        OidcUser oidcUser = buildOidcUser("testuser");
        AccountResponse account = new AccountResponse(1L, "testuser", "Ivan", "Petrov",
                LocalDate.of(1990, 1, 1), BigDecimal.valueOf(400), 0L);
        TransferResponse transferResponse = new TransferResponse("Transfer successful", BigDecimal.valueOf(400));

        when(accountClient.getAccountByLogin("testuser")).thenReturn(account);
        when(accountClient.getAllAccounts()).thenReturn(List.of(account));
        when(transferClient.processTransfer(1L, "otheruser", new BigDecimal("200"))).thenReturn(transferResponse);

        mockMvc.perform(post("/transfer")
                        .with(oidcLogin().oidcUser(oidcUser))
                        .with(csrf())
                        .param("value", "200")
                        .param("login", "otheruser"))
                .andExpect(status().isOk())
                .andExpect(view().name("main"))
                .andExpect(model().attribute("info", "Transfer successful"));
    }
}
