package com.samsepiol.portfolio.provider.beszel;

import lombok.Getter;

@Getter
public final class BeszelProviderException extends RuntimeException {
    private final BeszelProviderError error;

    public BeszelProviderException(BeszelProviderError error, Throwable cause) {
        super(error.name(), cause);
        this.error = error;
    }
}
