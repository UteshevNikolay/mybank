package com.my.pet.project.mybank.frontend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientRepository authorizedClientRepository) {
        DefaultOAuth2AuthorizedClientManager manager =
                new DefaultOAuth2AuthorizedClientManager(
                        clientRegistrationRepository, authorizedClientRepository);
        manager.setAuthorizedClientProvider(
                OAuth2AuthorizedClientProviderBuilder.builder()
                        .authorizationCode()
                        .refreshToken()
                        .build());
        return manager;
    }

    @Bean
    public RestClient accountsRestClient(@Value("${accounts.url}") String accountsUrl,
                                          OAuth2AuthorizedClientManager authorizedClientManager) {
        return buildRestClient(accountsUrl, authorizedClientManager);
    }

    @Bean
    public RestClient cashRestClient(@Value("${cash.url}") String cashUrl,
                                      OAuth2AuthorizedClientManager authorizedClientManager) {
        return buildRestClient(cashUrl, authorizedClientManager);
    }

    @Bean
    public RestClient transferRestClient(@Value("${transfer.url}") String transferUrl,
                                          OAuth2AuthorizedClientManager authorizedClientManager) {
        return buildRestClient(transferUrl, authorizedClientManager);
    }

    private RestClient buildRestClient(String baseUrl, OAuth2AuthorizedClientManager authorizedClientManager) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestInterceptor(oauth2Interceptor(authorizedClientManager))
                .build();
    }

    private ClientHttpRequestInterceptor oauth2Interceptor(OAuth2AuthorizedClientManager authorizedClientManager) {
        return (request, body, execution) -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
                OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                        .withClientRegistrationId(oauthToken.getAuthorizedClientRegistrationId())
                        .principal(authentication)
                        .build();
                OAuth2AuthorizedClient authorizedClient =
                        authorizedClientManager.authorize(authorizeRequest);
                if (authorizedClient != null) {
                    request.getHeaders().setBearerAuth(
                            authorizedClient.getAccessToken().getTokenValue());
                }
            }
            return execution.execute(request, body);
        };
    }
}
