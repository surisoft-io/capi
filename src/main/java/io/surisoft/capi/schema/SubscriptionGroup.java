package io.surisoft.capi.schema;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.Set;

public class  SubscriptionGroup {

    @JsonProperty("services")
    private Map<String, Set<String>> services;

    public Map<String, Set<String>> getServices() {
        return services;
    }

    public void setServices(Map<String, Set<String>> services) {
        this.services = services;
    }
}
