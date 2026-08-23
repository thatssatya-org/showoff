package com.samsepiol.portfolio.configuration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@ConfigurationProperties(prefix = "portfolio.github-token")
public record GitHubTokenProperties(
        @NotBlank String keyId,
        @NotBlank String keyBase64,
        @NotEmpty List<@NotBlank String> trustedProxyCidrs,
        @NotEmpty List<@NotBlank String> tailnetCidrs) {
}
