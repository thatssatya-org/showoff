package com.samsepiol.portfolio.provider;

import org.springframework.http.HttpStatus;

public enum GitHubProviderError {
    UNSUCCESSFUL_CONTRIBUTION_RESPONSE("github-contribution-response-unsuccessful", HttpStatus.BAD_GATEWAY),
    UNSUCCESSFUL_ACTIVITY_RESPONSE("github-activity-response-unsuccessful", HttpStatus.BAD_GATEWAY);

    private final String code;
    private final HttpStatus httpStatus;

    GitHubProviderError(String code, HttpStatus httpStatus) {
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
