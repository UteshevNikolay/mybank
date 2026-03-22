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
    public RestClient restClient(@Value("${gateway.url}") String gatewayUrl,
                                  OAuth2AuthorizedClientManager authorizedClientManager) {
        return RestClient.builder()
                .baseUrl(gatewayUrl)
                .requestInterceptor((request, body, execution) -> {
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
                })
                .build();
    }
}
