package io.surisoft.capi.schema;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class CapiEvent implements Serializable {
    private String id;
    private String type;
    private String key;
    private String value;
    private int nodeIndex;
    private String instanceId;

    @JsonProperty("throttleServiceObject")
    private ThrottleServiceObject throttleServiceObject;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public int getNodeIndex() {
        return nodeIndex;
    }

    public void setNodeIndex(int nodeIndex) {
        this.nodeIndex = nodeIndex;
    }

    public ThrottleServiceObject getThrottleServiceObject() {
        return throttleServiceObject;
    }

    public void setThrottleServiceObject(ThrottleServiceObject throttleServiceObject) {
        this.throttleServiceObject = throttleServiceObject;
    }
}
