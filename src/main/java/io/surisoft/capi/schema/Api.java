package io.surisoft.capi.schema;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Schema(hidden = true)
public class Api implements Serializable {
    @Id
    private String id;
    private String routeId;
    private String name;
    private String context;

    @OneToMany(targetEntity = Mapping.class, cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Set<Mapping> mappingList = new HashSet<>();

    private boolean roundRobinEnabled;
    private boolean failoverEnabled;
    private boolean matchOnUriPrefix;
    private HttpMethod httpMethod;
    private HttpProtocol httpProtocol;
    private String swaggerEndpoint;
    private int maximumFailoverAttempts;
    private boolean stickySession;
    private String stickySessionParam;
    private boolean stickySessionParamInCookie;
    private boolean removeMe;
    private boolean published;
    private boolean forwardPrefix;
    private boolean zipkinShowTraceId;
    private String zipkinServiceName;
    private String authorizationEndpointPublicKey;
    private boolean tenantAware;
    private boolean websocket;
    private List<String> subscriptionGroup;

    private boolean keepGroup;

    public boolean isSecured() {
        return secured;
    }

    public void setSecured(boolean secured) {
        this.secured = secured;
    }

    private boolean secured;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRouteId() {
        return routeId;
    }

    public void setRouteId(String routeId) {
        this.routeId = routeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public Set<Mapping> getMappingList() {
        return mappingList;
    }

    public void setMappingList(Set<Mapping> mappingList) {
        this.mappingList = mappingList;
    }

    public boolean isRoundRobinEnabled() {
        return roundRobinEnabled;
    }

    public void setRoundRobinEnabled(boolean roundRobinEnabled) {
        this.roundRobinEnabled = roundRobinEnabled;
    }

    public boolean isFailoverEnabled() {
        return failoverEnabled;
    }

    public void setFailoverEnabled(boolean failoverEnabled) {
        this.failoverEnabled = failoverEnabled;
    }

    public boolean isMatchOnUriPrefix() {
        return matchOnUriPrefix;
    }

    public void setMatchOnUriPrefix(boolean matchOnUriPrefix) {
        this.matchOnUriPrefix = matchOnUriPrefix;
    }

    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(HttpMethod httpMethod) {
        this.httpMethod = httpMethod;
    }

    public HttpProtocol getHttpProtocol() {
        return httpProtocol;
    }

    public void setHttpProtocol(HttpProtocol httpProtocol) {
        this.httpProtocol = httpProtocol;
    }

    public String getSwaggerEndpoint() {
        return swaggerEndpoint;
    }

    public void setSwaggerEndpoint(String swaggerEndpoint) {
        this.swaggerEndpoint = swaggerEndpoint;
    }

    public int getMaximumFailoverAttempts() {
        return maximumFailoverAttempts;
    }

    public void setMaximumFailoverAttempts(int maximumFailoverAttempts) {
        this.maximumFailoverAttempts = maximumFailoverAttempts;
    }

    public boolean isStickySession() {
        return stickySession;
    }

    public void setStickySession(boolean stickySession) {
        this.stickySession = stickySession;
    }

    public String getStickySessionParam() {
        return stickySessionParam;
    }

    public void setStickySessionParam(String stickySessionParam) {
        this.stickySessionParam = stickySessionParam;
    }

    public boolean isStickySessionParamInCookie() {
        return stickySessionParamInCookie;
    }

    public void setStickySessionParamInCookie(boolean stickySessionParamInCookie) {
        this.stickySessionParamInCookie = stickySessionParamInCookie;
    }

    public boolean isRemoveMe() {
        return removeMe;
    }

    public void setRemoveMe(boolean removeMe) {
        this.removeMe = removeMe;
    }

    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    public boolean isForwardPrefix() {
        return forwardPrefix;
    }

    public void setForwardPrefix(boolean forwardPrefix) {
        this.forwardPrefix = forwardPrefix;
    }

    public boolean isZipkinShowTraceId() {
        return zipkinShowTraceId;
    }

    public void setZipkinShowTraceId(boolean zipkinShowTraceId) {
        this.zipkinShowTraceId = zipkinShowTraceId;
    }

    public String getZipkinServiceName() {
        return zipkinServiceName;
    }

    public void setZipkinServiceName(String zipkinServiceName) {
        this.zipkinServiceName = zipkinServiceName;
    }

    public String getAuthorizationEndpointPublicKey() {
        return authorizationEndpointPublicKey;
    }

    public void setAuthorizationEndpointPublicKey(String authorizationEndpointPublicKey) {
        this.authorizationEndpointPublicKey = authorizationEndpointPublicKey;
    }

    public boolean isTenantAware() {
        return tenantAware;
    }

    public void setTenantAware(boolean tenantAware) {
        this.tenantAware = tenantAware;
    }

    public boolean isWebsocket() {
        return websocket;
    }

    public void setWebsocket(boolean websocket) {
        this.websocket = websocket;
    }

    public List<String> getSubscriptionGroup() {
        return subscriptionGroup;
    }

    public void setSubscriptionGroup(List<String> subscriptionGroup) {
        this.subscriptionGroup = subscriptionGroup;
    }

    public boolean isKeepGroup() {
        return keepGroup;
    }

    public void setKeepGroup(boolean keepGroup) {
        this.keepGroup = keepGroup;
    }
}