package io.surisoft.capi.schema;

import java.io.Serializable;

public class ThrottleServiceObject implements Serializable {

    private final String serviceId;
    private final long totalCallsAllowed;
    private long currentCalls;
    private final long expirationTime;
    private final long expirationDuration;

    public ThrottleServiceObject(String serviceId, long expirationDuration, long totalCallsAllowed) {
        this.serviceId = serviceId;
        this.totalCallsAllowed = totalCallsAllowed;
        this.expirationDuration = expirationDuration;
        this.expirationTime = System.currentTimeMillis() + expirationDuration;
    }

    public boolean isObjectExpired() {
        return System.currentTimeMillis() > expirationTime;
    }

    public long remainingTime() {
        if(!isObjectExpired()) {
            return expirationTime - System.currentTimeMillis();
        }
        return 0;
    }

    public long getExpirationDuration() {
        return expirationDuration;
    }

    public String getServiceId() {
        return serviceId;
    }

    public long getTotalCallsAllowed() {
        return totalCallsAllowed;
    }

    public long getCurrentCalls() {
        return currentCalls;
    }

    public void setCurrentCalls(long currentCalls) {
        this.currentCalls = currentCalls;
    }
}