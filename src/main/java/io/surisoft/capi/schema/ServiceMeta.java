package io.surisoft.capi.schema;

import com.fasterxml.jackson.annotation.*;

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
    private boolean allowSubscriptions;

    @JsonProperty("allowed-origins")
    private String allowedOrigins;

    @JsonProperty("keep-group")
    private boolean keepGroup;

    private String openApiEndpoint;

    private String opaRego;

    @JsonProperty("capi-instance")
    private String capiNamespace;

    @JsonProperty("namespace")
    private String namespace;

    @JsonProperty("route-group-first")
    private boolean routeGroupFirst;

    private boolean throttle;
    private boolean throttleGlobal;
    private long throttleTotalCalls = -1;
    private long throttleDuration = -1;
    private boolean rateLimit;

    @JsonProperty("expose-open-api-definition")
    private boolean exposeOpenApiDefinition;
    @JsonProperty("secure-open-api-definition")
    private boolean secureOpenApiDefinition;
    @JsonProperty("state")
    private State state;

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

    @JsonGetter("allowSubscriptions")
    public boolean isAllowSubscriptions() {
        return allowSubscriptions;
    }

    @JsonSetter("allow-subscriptions")
    public void setAllowSubscriptions(boolean allowSubscriptions) {
        this.allowSubscriptions = allowSubscriptions;
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
        if(capiNamespace != null && !capiNamespace.isEmpty()) {
            return capiNamespace;
        } else if(namespace != null && !namespace.isEmpty()) {
            return namespace;
        }
        return null;
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

    public boolean isThrottle() {
        return throttle;
    }

    public void setThrottle(boolean throttle) {
        this.throttle = throttle;
    }

    public boolean isRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(boolean rateLimit) {
        this.rateLimit = rateLimit;
    }

    public boolean isThrottleGlobal() {
        return throttleGlobal;
    }

    public void setThrottleGlobal(boolean throttleGlobal) {
        this.throttleGlobal = throttleGlobal;
    }

    public long getThrottleTotalCalls() {
        return throttleTotalCalls;
    }

    public void setThrottleTotalCalls(long throttleTotalCalls) {
        this.throttleTotalCalls = throttleTotalCalls;
    }

    public long getThrottleDuration() {
        return throttleDuration;
    }

    public void setThrottleDuration(long throttleDuration) {
        this.throttleDuration = throttleDuration;
    }

    public String getCapiNamespace() {
        return capiNamespace;
    }

    public void setCapiNamespace(String capiNamespace) {
        this.capiNamespace = capiNamespace;
    }

    public boolean isExposeOpenApiDefinition() {
        return exposeOpenApiDefinition;
    }

    public void setExposeOpenApiDefinition(boolean exposeOpenApiDefinition) {
        this.exposeOpenApiDefinition = exposeOpenApiDefinition;
    }

    public boolean isSecureOpenApiDefinition() {
        return secureOpenApiDefinition;
    }

    public void setSecureOpenApiDefinition(boolean secureOpenApiDefinition) {
        this.secureOpenApiDefinition = secureOpenApiDefinition;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }
}