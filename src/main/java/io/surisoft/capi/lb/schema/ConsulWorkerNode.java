package io.surisoft.capi.lb.schema;

import java.io.Serializable;
import java.util.Date;

public class ConsulWorkerNode implements Serializable {
    private String member;
    private Date lastSync;

    public String getMember() {
        return member;
    }

    public void setMember(String member) {
        this.member = member;
    }

    public Date getLastSync() {
        return lastSync;
    }

    public void setLastSync(Date lastSync) {
        this.lastSync = lastSync;
    }
}