package io.surisoft.capi.utils;

public class ErrorMessage {
    private ErrorMessage() {
        throw new IllegalStateException("Utility class");
    }
    public static final String ERROR_CONNECTING_TO_CONSUL = "Error connecting to Consul, will try again...";
    public static final String IS_AUTHORIZED = "{} is authorized!";
    public static final String IS_NOT_AUTHORIZED = "{} is not authorized!";
    public static final String IS_NOT_PRESENT = "{} is not present!";
    public static final String NO_TOKEN_PROVIDED = "No token provided!";
}
