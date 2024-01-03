package io.surisoft.capi.schema;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OpaResult {
    private boolean result;

    public boolean isAllowed() {
        return result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }
}
