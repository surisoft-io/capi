package io.surisoft.capi.schema;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;

public class ThrottleServiceObject implements Serializable {

    private String serviceId;
    private String consumerKey;
    private long totalCallsAllowed;
    private long currentCalls;
    private long expirationTime;

    public ThrottleServiceObject() {}

    public ThrottleServiceObject(String serviceId, String consumerKey, long totalCallsAllowed, long expirationDuration) {
        this.serviceId = serviceId;
        this.consumerKey = consumerKey;
        this.totalCallsAllowed = totalCallsAllowed;
        this.currentCalls = 1;
        this.expirationTime = System.currentTimeMillis() + expirationDuration;
    }

    @JsonIgnore
    public String getCacheKey() {
        return serviceId;
    }

    public String getServiceId() {
        return serviceId;
    }

    public long getCurrentCalls() {
        return currentCalls;
    }

    @JsonIgnore
    public boolean isExpired() {
        return System.currentTimeMillis() > expirationTime;
    }

    @JsonIgnore
    public boolean canCall() {
        return !isExpired() && currentCalls < totalCallsAllowed;
    }

    @JsonIgnore
    public void increment() {
        currentCalls++;
    }

    public long getExpirationTime() {
        return expirationTime;
    }

    public long getTotalCallsAllowed() {
        return totalCallsAllowed;
    }

    public String getConsumerKey() {
        return consumerKey;
    }
}