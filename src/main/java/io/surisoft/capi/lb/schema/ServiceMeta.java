package io.surisoft.capi.lb.schema;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceMeta {

    @JsonProperty("root-context")
    private String rootContext;

    @JsonProperty("schema")
    private String schema;

    @JsonProperty("secured")
    private boolean secured;

    @JsonProperty("tenant_aware")
    private boolean tenantAware;

    @JsonProperty("tenant_id")
    private String tenantId;

    public boolean isSecured() {
        return secured;
    }

    public void setSecured(boolean secured) {
        this.secured = secured;
    }

    public String getRootContext() {
        return rootContext;
    }

    public void setRootContext(String rootContext) {
        this.rootContext = rootContext;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public boolean isTenantAware() {
        return tenantAware;
    }

    public void setTenantAware(boolean tenantAware) {
        this.tenantAware = tenantAware;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
}