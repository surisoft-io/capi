package io.surisoft.capi.lb.schema;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class ConsulWorkerNode implements Serializable {
    private String member;
    private Date lastSync;
}