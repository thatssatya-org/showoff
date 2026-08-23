package com.samsepiol.portfolio.configuration;

import com.samsepiol.library.http.client.HttpClient;
import com.samsepiol.library.http.config.HttpConfig;
import com.samsepiol.library.http.config.HttpConfigService;
import com.samsepiol.portfolio.application.ListmonkNewsletterSubscriptionService;
import com.samsepiol.portfolio.application.NewsletterSubscriptionService;
import com.samsepiol.portfolio.application.UnavailableNewsletterSubscriptionService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import java.net.URI;
import java.util.Map;

@Configuration
public class NewsletterSubscriptionConfiguration {

    @Bean
    @ConditionalOnMissingBean(NewsletterSubscriptionService.class)
    NewsletterSubscriptionService unavailableNewsletterSubscriptionService() {
        return new UnavailableNewsletterSubscriptionService();
    }

    @Configuration
    @EnableConfigurationProperties(ListmonkProperties.class)
    @ConditionalOnProperty(prefix = "portfolio.listmonk", name = {"base-url", "username", "password", "list-id"})
    public static class ListmonkConfiguration {
        public static final String SERVICE = "listmonk";
        public static final String CREATE_SUBSCRIBER = "create-subscriber";

        @Bean
        HttpConfigService listmonkHttpConfigService(ListmonkProperties properties) {
            return () -> Map.of(SERVICE, listmonkServiceConfig(properties));
        }

        @Bean
        NewsletterSubscriptionService listmonkNewsletterSubscriptionService(
                HttpClient httpClient,
                ListmonkProperties properties) {
            return new ListmonkNewsletterSubscriptionService(httpClient, properties);
        }

        private static HttpConfig.ServiceConfig listmonkServiceConfig(ListmonkProperties properties) {
            var uri = URI.create(properties.baseUrl());
            if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())
                    || uri.getRawUserInfo() != null || uri.getHost() == null || uri.getRawQuery() != null
                    || uri.getRawFragment() != null) {
                throw new IllegalArgumentException("Listmonk base URL must be an absolute HTTP(S) URL without credentials, query, or fragment");
            }
            var serviceConfig = new HttpConfig.ServiceConfig();
            var pathPrefix = uri.getRawPath() == null || "/".equals(uri.getRawPath()) ? "" : uri.getRawPath();
            serviceConfig.setBaseUrl(uri.getRawAuthority() + pathPrefix);
            serviceConfig.setSecured("https".equalsIgnoreCase(uri.getScheme()));
            var createSubscriber = new HttpConfig.ServiceConfig.ApiConfig();
            createSubscriber.setMethod(HttpMethod.POST);
            createSubscriber.setPath("/api/subscribers");
            serviceConfig.setApiConfigs(Map.of(CREATE_SUBSCRIBER, createSubscriber));
            return serviceConfig;
        }
    }
}
