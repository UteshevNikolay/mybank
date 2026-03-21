package com.my.pet.project.mybank.frontend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient restClient(@Value("${gateway.url}") String gatewayUrl) {
        return RestClient.builder()
                .baseUrl(gatewayUrl)
                .build();
    }
}
