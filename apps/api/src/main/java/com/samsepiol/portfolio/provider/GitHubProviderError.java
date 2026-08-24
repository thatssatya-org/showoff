package com.samsepiol.portfolio.provider;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum GitHubProviderError {
    UNSUCCESSFUL_CONTRIBUTION_RESPONSE("github-contribution-response-unsuccessful", HttpStatus.BAD_GATEWAY),
    UNSUCCESSFUL_ACTIVITY_RESPONSE("github-activity-response-unsuccessful", HttpStatus.BAD_GATEWAY),
    UNSUCCESSFUL_REPOSITORY_RESPONSE("github-repository-response-unsuccessful", HttpStatus.BAD_GATEWAY);

    private final String code;
    private final HttpStatus httpStatus;
}
