package io.surisoft.capi.scim.exception;

public class ScimResourceInvalidException extends RuntimeException {
  public ScimResourceInvalidException(String message) {
    super(message);
  }
  public ScimResourceInvalidException(String message, Throwable cause) {
    super(message, cause);
  }
}