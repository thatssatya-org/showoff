package com.samsepiol.portfolio.provider.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Value;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubRepositoryGraphQlErrorResponse {
}
