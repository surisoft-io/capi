package io.surisoft.capi.schema;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.undertow.server.HttpHandler;

import java.util.Set;

public class SSEClient {

    private String apiId;
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

    public void setRequiresSubscription(boolean requiresSubscription) {
        this.requiresSubscription = requiresSubscription;
    }

    public String getSubscriptionRole() {
        return subscriptionRole;
    }

    public void setSubscriptionRole(String subscriptionRole) {
        this.subscriptionRole = subscriptionRole;
    }

    public String getApiId() {
        return apiId;
    }

    public void setApiId(String apiId) {
        this.apiId = apiId;
    }

    public Set<Mapping> getMappingList() {
        return mappingList;
    }

    public void setMappingList(Set<Mapping> mappingList) {
        this.mappingList = mappingList;
    }
}
