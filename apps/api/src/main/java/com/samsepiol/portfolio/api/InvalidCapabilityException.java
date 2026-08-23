package com.samsepiol.portfolio.api;

public class InvalidCapabilityException extends RuntimeException {
    public InvalidCapabilityException() {
        super("The requested capability is not recognised");
    }
}
