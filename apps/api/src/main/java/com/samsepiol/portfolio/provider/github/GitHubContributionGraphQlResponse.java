package com.samsepiol.portfolio.provider.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Value
@Builder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubContributionGraphQlResponse {
    GitHubContributionGraphQlDataResponse data;
    List<GitHubContributionGraphQlErrorResponse> errors;

    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }

    public boolean viewerMatches(String expectedHandle) {
        return data != null && data.getViewer() != null && data.getViewer().getLogin() != null
                && data.getViewer().getLogin().equalsIgnoreCase(expectedHandle);
    }
}
