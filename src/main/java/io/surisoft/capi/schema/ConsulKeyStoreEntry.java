package io.surisoft.capi.schema;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ConsulKeyStoreEntry {
    @JsonProperty("LocalIndex")
    private int localIndex;
    @JsonProperty("Key")
    private String key;
    @JsonProperty("Flags")
    private int flags;
    @JsonProperty("Value")
    private String value;
    @JsonProperty("CreateIndex")
    private int createIndex;
    @JsonProperty("ModifyIndex")
    private int modifyIndex;
    private Set<String> servicesProcessed;

    public int getLocalIndex() {
        return localIndex;
    }

    public void setLocalIndex(int localIndex) {
        this.localIndex = localIndex;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public int getFlags() {
        return flags;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public int getCreateIndex() {
        return createIndex;
    }

    public void setCreateIndex(int createIndex) {
        this.createIndex = createIndex;
    }

    public int getModifyIndex() {
        return modifyIndex;
    }

    public void setModifyIndex(int modifyIndex) {
        this.modifyIndex = modifyIndex;
    }
    public Set<String> getServicesProcessed() {
        return servicesProcessed;
    }
    public void setServicesProcessed(Set<String> servicesProcessed) {
        this.servicesProcessed = servicesProcessed;
    }
}
