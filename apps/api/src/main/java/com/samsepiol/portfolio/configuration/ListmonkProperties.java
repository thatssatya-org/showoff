package com.samsepiol.portfolio.configuration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "portfolio.listmonk")
public record ListmonkProperties(
        @NotBlank String baseUrl,
        @NotBlank String username,
        @NotBlank String password,
        @Positive long listId) {
}
