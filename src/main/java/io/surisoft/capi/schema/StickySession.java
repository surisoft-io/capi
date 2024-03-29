package io.surisoft.capi.schema;


import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

@Schema(hidden = true)
public class StickySession implements Serializable {
    private String id;
    private String paramName;
    private String paramValue;
    private int nodeIndex;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getParamName() {
        return paramName;
    }

    public void setParamName(String paramName) {
        this.paramName = paramName;
    }

    public String getParamValue() {
        return paramValue;
    }

    public void setParamValue(String paramValue) {
        this.paramValue = paramValue;
    }

    public int getNodeIndex() {
        return nodeIndex;
    }

    public void setNodeIndex(int nodeIndex) {
        this.nodeIndex = nodeIndex;
    }
}