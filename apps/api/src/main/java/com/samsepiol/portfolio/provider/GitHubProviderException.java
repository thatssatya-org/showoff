package com.samsepiol.portfolio.provider;

import lombok.Getter;
import lombok.NonNull;

@Getter
public final class GitHubProviderException extends RuntimeException {
    private final GitHubProviderError error;

    public GitHubProviderException(@NonNull GitHubProviderError error) {
        super(error.getCode());
        this.error = error;
    }
}
