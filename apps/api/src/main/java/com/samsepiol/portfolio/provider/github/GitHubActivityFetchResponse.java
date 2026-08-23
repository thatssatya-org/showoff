package com.samsepiol.portfolio.provider.github;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class GitHubActivityFetchResponse {
    @NonNull Integer statusCode;
    String etag;
    @NonNull List<GitHubPublicEventResponse> events;

    public boolean isNotModified() {
        return statusCode == 304;
    }

    public boolean isSuccessful() {
        return statusCode >= 200 && statusCode < 300;
    }
}
