package com.samsepiol.portfolio.provider.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.Objects;

@Value
@Builder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubContributionGraphQlResponse {
    GitHubContributionGraphQlDataResponse data;
    List<GitHubContributionGraphQlErrorResponse> errors;

    public boolean hasErrors() {
        return CollectionUtils.isNotEmpty(errors);
    }

    public boolean viewerMatches(String expectedHandle) {
        return Objects.nonNull(data) && Objects.nonNull(data.getViewer()) && Objects.nonNull(data.getViewer().getLogin())
                && data.getViewer().getLogin().equalsIgnoreCase(expectedHandle);
    }
}
