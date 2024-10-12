package io.surisoft.capi.utils;

public class ErrorMessage {
    private ErrorMessage() {
        throw new IllegalStateException("Utility class");
    }
    public static final String ERROR_CONNECTING_TO_CONSUL = "Error connecting to Consul, will try again...";
}
