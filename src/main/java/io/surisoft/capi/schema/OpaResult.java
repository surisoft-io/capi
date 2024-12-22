package io.surisoft.capi.schema;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OpaResult {
    private boolean result;
    private long totalCallsAllowed = -1;
    private long duration = -1;
    private String consumerKey;

    public boolean isAllowed() {
        return result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }
    public long getTotalCallsAllowed() {
        return totalCallsAllowed;
    }
    public void setTotalCallsAllowed(long totalCallsAllowed) {
        this.totalCallsAllowed = totalCallsAllowed;
    }
    public long getDuration() {
        return duration;
    }
    public void setDuration(long duration) {
        this.duration = duration;
    }
    public String getConsumerKey() {
        return consumerKey;
    }
    public void setConsumerKey(String consumerKey) {
        this.consumerKey = consumerKey;
    }
}
