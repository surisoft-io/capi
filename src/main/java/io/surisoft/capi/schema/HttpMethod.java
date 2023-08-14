package io.surisoft.capi.schema;

public enum HttpMethod {
    ALL("all"),
    GET("get"),
    POST("post"),
    PUT("put"),
    DELETE("delete"),
    PATCH("patch");

    private String method;
    HttpMethod(String method) {
        this.method = method;
    }

    public String getMethod() {
        return this.method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

}
