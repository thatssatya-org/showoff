package com.samsepiol.portfolio.configuration;

import com.samsepiol.library.http.client.HttpClient;
import com.samsepiol.library.http.client.impl.DefaultHttpClient;
import com.samsepiol.library.http.config.HttpConfig;
import com.samsepiol.library.http.config.HttpConfigService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;

import java.util.Map;

@Configuration
@ConditionalOnProperty(prefix = "portfolio.github-refresh", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(GitHubRefreshProperties.class)
public class GitHubRefreshConfiguration {
    public static final String SERVICE = "github";
    public static final String PUBLIC_EVENTS = "public-events";
    public static final String CONTRIBUTIONS = "contributions";
    public static final String REPOSITORY = "repository";

    @Bean
    HttpConfigService gitHubHttpConfigService(GitHubRefreshProperties properties) {
        return () -> Map.of(SERVICE, gitHubServiceConfig(properties));
    }

    @Bean(name = "gitHubHttpClient", destroyMethod = "close")
    HttpClient gitHubHttpClient(java.util.List<HttpConfigService> services) {
        return new DefaultHttpClient(new HttpConfig(services));
    }

    private static HttpConfig.ServiceConfig gitHubServiceConfig(GitHubRefreshProperties properties) {
        var service = new HttpConfig.ServiceConfig();
        service.setBaseUrl("api.github.com");
        service.setSecured(true);
        var publicEvents = new HttpConfig.ServiceConfig.ApiConfig();
        publicEvents.setMethod(HttpMethod.GET);
        publicEvents.setPath("/users/" + properties.handle() + "/events/public?per_page=8");
        publicEvents.setRequestLoggingEnabled(false);
        publicEvents.setResponseLoggingEnabled(false);
        publicEvents.setMaxResponseBodyBytes(262_144);
        var contributions = new HttpConfig.ServiceConfig.ApiConfig();
        contributions.setMethod(HttpMethod.POST);
        contributions.setPath("/graphql");
        contributions.setRequestLoggingEnabled(false);
        contributions.setResponseLoggingEnabled(false);
        contributions.setMaxResponseBodyBytes(262_144);
        var repository = new HttpConfig.ServiceConfig.ApiConfig();
        repository.setMethod(HttpMethod.GET);
        repository.setPath("/repos/" + properties.repositoryOwner() + "/" + properties.repositoryName());
        repository.setRequestLoggingEnabled(false);
        repository.setResponseLoggingEnabled(false);
        repository.setMaxResponseBodyBytes(262_144);
        service.setApiConfigs(Map.of(PUBLIC_EVENTS, publicEvents, CONTRIBUTIONS, contributions, REPOSITORY, repository));
        return service;
    }
}
