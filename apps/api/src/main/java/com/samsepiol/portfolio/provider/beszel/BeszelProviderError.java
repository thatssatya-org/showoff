package com.samsepiol.portfolio.provider.beszel;

import org.springframework.http.HttpStatus;

public enum BeszelProviderError {
    UPSTREAM_UNAVAILABLE(HttpStatus.BAD_GATEWAY);

    private final HttpStatus httpStatus;

    BeszelProviderError(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }
}
