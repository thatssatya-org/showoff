package com.samsepiol.portfolio.provider.github;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GitHubRepositoryFetchResponse {
    int statusCode;
    GitHubRepositorySnapshotResponse repository;
    boolean hasErrors;

    public boolean isSuccessful() {
        return statusCode >= 200 && statusCode < 300 && !hasErrors && repository != null;
    }
}
