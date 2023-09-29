package io.surisoft.capi.schema;

import io.surisoft.capi.kafka.CapiInstance;

import java.io.Serializable;

public class CapiEvent implements Serializable {
    private String id;
    private String type;
    private String key;
    private String value;
    private int nodeIndex;

    private CapiInstance instanceId;

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

    public CapiInstance getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(CapiInstance instanceId) {
        this.instanceId = instanceId;
    }

    public int getNodeIndex() {
        return nodeIndex;
    }

    public void setNodeIndex(int nodeIndex) {
        this.nodeIndex = nodeIndex;
    }
}
