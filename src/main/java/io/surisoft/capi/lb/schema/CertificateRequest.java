package io.surisoft.capi.lb.schema;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(hidden = true)
public class CertificateRequest implements Serializable {
    private String url;
    private int port = 443;
    private String alias;
}
