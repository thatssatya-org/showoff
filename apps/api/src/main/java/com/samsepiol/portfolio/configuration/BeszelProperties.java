package com.samsepiol.portfolio.configuration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "portfolio.beszel")
public record BeszelProperties(
        @NotNull Boolean enabled,
        @NotBlank String baseUrl,
        @NotNull Duration cacheTtl) {

    public URI endpoint() {
        var endpoint = URI.create(baseUrl);
        if (!"https".equalsIgnoreCase(endpoint.getScheme()) || endpoint.getHost() == null
                || endpoint.getRawUserInfo() != null || endpoint.getRawQuery() != null || endpoint.getRawFragment() != null
                || !(endpoint.getHost().endsWith(".ts.net") || endpoint.getHost().matches("100\\..+"))) {
            throw new IllegalArgumentException("portfolio.beszel.base-url must be a Tailnet HTTPS origin");
        }
        return endpoint;
    }
}
