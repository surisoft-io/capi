package io.surisoft.capi.lb.schema;

public class RunningTenant {
    private String tenant;
    private int nodeIndex;

    public RunningTenant(String tenant, int index) {
        this.tenant = tenant;
        this.nodeIndex = index;
    }

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    public int getNodeIndex() {
        return nodeIndex;
    }

    public void setNodeIndex(int nodeIndex) {
        this.nodeIndex = nodeIndex;
    }
}
