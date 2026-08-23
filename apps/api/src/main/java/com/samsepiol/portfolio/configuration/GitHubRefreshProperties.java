package com.samsepiol.portfolio.configuration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "portfolio.github-refresh")
public record GitHubRefreshProperties(
        boolean enabled,
        boolean publicApproved,
        @NotBlank String profileId,
        @NotBlank @Pattern(regexp = "[A-Za-z0-9-]{1,39}") String handle,
        @NotBlank @Pattern(regexp = "[A-Za-z0-9-]{1,39}") String repositoryOwner,
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_.-]{1,100}") String repositoryName,
        @NotBlank String cron) {
}
