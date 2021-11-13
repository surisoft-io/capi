package io.surisoft.capi.lb.schema;

import lombok.Data;

import java.io.Serializable;

@Data
public class MappingId implements Serializable {
    private String rootContext;
    private String hostname;
    private int port;
}
