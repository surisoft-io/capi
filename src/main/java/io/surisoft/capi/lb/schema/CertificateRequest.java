package io.surisoft.capi.lb.schema;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

@Schema(hidden = true)
public class CertificateRequest implements Serializable {
    private String url;
    private int port = 443;
    private String alias;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }
}