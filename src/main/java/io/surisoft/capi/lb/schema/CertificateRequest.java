package io.surisoft.capi.lb.schema;

import lombok.Data;

import java.io.Serializable;

@Data
public class CertificateRequest implements Serializable {
    private String url;
    private int port = 443;
    private String alias;
}
