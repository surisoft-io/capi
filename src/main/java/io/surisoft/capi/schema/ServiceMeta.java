package io.surisoft.capi.schema;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

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

        @JsonProperty("group")
        private String group;

        @JsonProperty("X-B3-TraceId")
        private boolean b3TraceId;

        @JsonProperty("ingress")
        private String ingress;

        @JsonProperty("sticky_session_enabled")
        private boolean stickySession;

        @JsonProperty("sticky_session_type")
        private String stickySessionType;

        @JsonProperty("sticky_session_key")
        private String stickySessionKey;

        @JsonProperty("type")
        private String type;

        private String subscriptionGroup;

        @JsonProperty("allowed-origins")
        private String allowedOrigins;

        @JsonProperty("keep-group")
        private boolean keepGroup;

        private String openApiEndpoint;

        private String opaRego;

        private String namespace;

        @JsonProperty("route-group-first")
        private boolean routeGroupFirst;

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

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public boolean isB3TraceId() {
        return b3TraceId;
    }

    public void setB3TraceId(boolean b3TraceId) {
        this.b3TraceId = b3TraceId;
    }

    public String getIngress() {
        return ingress;
    }

    public void setIngress(String ingress) {
        this.ingress = ingress;
    }

    public boolean isStickySession() {
        return stickySession;
    }

    public void setStickySession(boolean stickySession) {
        this.stickySession = stickySession;
    }

    public String getStickySessionType() {
        return stickySessionType;
    }

    public void setStickySessionType(String stickySessionType) {
        this.stickySessionType = stickySessionType;
    }

    public String getStickySessionKey() {
        return stickySessionKey;
    }

    public void setStickySessionKey(String stickySessionKey) {
        this.stickySessionKey = stickySessionKey;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @JsonGetter("subscriptionGroup")
    public String getSubscriptionGroup() {
        return subscriptionGroup;
    }

    @JsonSetter("subscription-group")
    public void setSubscriptionGroup(String subscriptionGroup) {
        this.subscriptionGroup = subscriptionGroup;
    }

    public boolean isKeepGroup() {
        return keepGroup;
    }

    public void setKeepGroup(boolean keepGroup) {
        this.keepGroup = keepGroup;
    }

    public String getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(String allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    @JsonGetter("openApi")
    public String getOpenApiEndpoint() {
        return openApiEndpoint;
    }

    @JsonSetter("open-api")
    public void setOpenApiEndpoint(String openApiEndpoint) {
        this.openApiEndpoint = openApiEndpoint;
    }

    @JsonGetter("opaRego")
    public String getOpaRego() {
        return opaRego;
    }

    @JsonSetter("opa-rego")
    public void setOpaRego(String opaRego) {
        this.opaRego = opaRego;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public boolean isRouteGroupFirst() {
        return routeGroupFirst;
    }

    public void setRouteGroupFirst(boolean routeGroupFirst) {
        this.routeGroupFirst = routeGroupFirst;
    }
}