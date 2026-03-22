package com.my.pet.project.mybank.cash.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    @LoadBalanced
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    public RestClient accountsRestClient(RestClient.Builder builder) {
        return builder.clone().baseUrl("http://accounts").build();
    }

    @Bean
    public RestClient notificationsRestClient(RestClient.Builder builder) {
        return builder.clone().baseUrl("http://notifications").build();
    }
}
