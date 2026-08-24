package com.samsepiol.portfolio.provider.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubRepositorySnapshotResponse {
    String name;
    String nameWithOwner;
    Boolean isPrivate;
    String visibility;
    String url;
    Integer stargazerCount;
    GitHubRepositoryLanguageResponse primaryLanguage;
    GitHubRepositoryBranchResponse defaultBranchRef;
    GitHubRepositoryReleaseResponse latestRelease;

    public boolean matches(GitHubRepositoryRequest request) {
        return (request.getExpectedOwner() + "/" + request.getExpectedName()).equalsIgnoreCase(nameWithOwner)
                && Boolean.FALSE.equals(isPrivate) && "PUBLIC".equals(visibility);
    }
}
