package io.surisoft.capi.schema;

import com.fasterxml.jackson.annotation.*;

import java.util.HashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceMeta {

    @JsonProperty("root-context")
    private String rootContext;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Deprecated
    private String schema;

    @JsonProperty("scheme")
    private String scheme;

    @JsonProperty("secured")
    private boolean secured;

    @JsonProperty("group")
    private String group;

    @JsonProperty("X-B3-TraceId")
    private boolean b3TraceId;

    @JsonProperty("ingress")
    private String ingress;

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

    private Map<String, String> extraServiceMeta = new HashMap<>();

    @JsonIgnore
    private Map<String, String> unknownProperties = new HashMap<>();

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

    public String getScheme() {
        if(scheme != null && !scheme.isEmpty()) {
            return scheme;
        } else if(schema != null && !schema.isEmpty()) {
            scheme = schema;
            schema = null;
            return scheme;
        }
        return null;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
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

    @JsonAnySetter
    public void handleUnknown(String key, String value) {
        unknownProperties.put(key, value);
    }

    @JsonAnyGetter
    public Map<String, String> getUnknownProperties() {
        return unknownProperties;
    }

    public Map<String, String> getExtraServiceMeta() {
        return extraServiceMeta;
    }

    public void addExtraServiceMeta(String key, String value) {
        if(extraServiceMeta == null) {
            extraServiceMeta = new HashMap<>();
        }
        extraServiceMeta.put(key, value);
    }
}