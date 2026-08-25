package com.samsepiol.portfolio.configuration;

import com.samsepiol.library.http.config.HttpConfig;
import com.samsepiol.library.http.config.HttpConfigService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import java.util.Map;

/**
 * Registers the provider origin only in the backend HTTP client. The web
 * container has neither this origin nor a route that can reach it directly.
 */
@Configuration
@ConditionalOnProperty(prefix = "portfolio.beszel", name = "enabled", havingValue = "true")
public class BeszelHttpConfiguration {
    public static final String SERVICE = "beszel";
    public static final String SYSTEMS_API = "systems";
    public static final String LATEST_STATS_API = "latest-stats";

    @Bean
    HttpConfigService beszelHttpConfigService(BeszelProperties properties) {
        var endpoint = properties.endpoint();
        var service = new HttpConfig.ServiceConfig();
        service.setBaseUrl(endpoint.getRawAuthority()
                + (endpoint.getRawPath() == null || "/".equals(endpoint.getRawPath()) ? "" : endpoint.getRawPath()));
        service.setSecured(true);
        service.setApiConfigs(Map.of(
                SYSTEMS_API, api(HttpMethod.GET, "/api/collections/systems/records?perPage=32&fields=id,name,status", 131_072),
                LATEST_STATS_API, api(HttpMethod.GET,
                        "/api/collections/system_stats/records?perPage=256&sort=-created&fields=system,stats,created", 262_144)));
        return () -> Map.of(SERVICE, service);
    }

    private static HttpConfig.ServiceConfig.ApiConfig api(HttpMethod method, String path, int maxResponseBodyBytes) {
        var api = new HttpConfig.ServiceConfig.ApiConfig();
        api.setMethod(method);
        api.setPath(path);
        api.setRequestLoggingEnabled(false);
        api.setResponseLoggingEnabled(false);
        api.setConnectionTimeoutMs(2_000);
        api.setReadTimeoutMs(4_000);
        api.setMaxResponseBodyBytes(maxResponseBodyBytes);
        return api;
    }
}
