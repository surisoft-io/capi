package io.surisoft.capi.schema;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.undertow.server.HttpHandler;

import java.util.Set;

public class WebsocketClient {

    private String serviceId;
    private String path;
    private Set<Mapping> mappingList;
    @JsonIgnore
    private HttpHandler httpHandler;
    private boolean requiresSubscription;
    private String subscriptionRole;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public HttpHandler getHttpHandler() {
        return httpHandler;
    }

    public void setHttpHandler(HttpHandler httpHandler) {
        this.httpHandler = httpHandler;
    }

    public boolean requiresSubscription() {
        return requiresSubscription;
    }

    public boolean isRequiresSubscription() {
        return requiresSubscription;
    }

    public void setRequiresSubscription(boolean requiresSubscription) {
        this.requiresSubscription = requiresSubscription;
    }

    public String getSubscriptionRole() {
        return subscriptionRole;
    }

    public void setSubscriptionRole(String subscriptionRole) {
        this.subscriptionRole = subscriptionRole;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public Set<Mapping> getMappingList() {
        return mappingList;
    }

    public void setMappingList(Set<Mapping> mappingList) {
        this.mappingList = mappingList;
    }
}
