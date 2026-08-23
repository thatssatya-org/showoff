package com.samsepiol.portfolio.configuration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@Data
@ConfigurationProperties(prefix = "portfolio.github-refresh")
public class GitHubRefreshProperties {
    @NotNull
    private Boolean enabled;
    @NotNull
    private Boolean publicApproved;
    @NotBlank
    private String profileId;
    @NotBlank
    @Pattern(regexp = "[A-Za-z0-9-]{1,39}")
    private String handle;
    @NotBlank
    private String cron;
}
