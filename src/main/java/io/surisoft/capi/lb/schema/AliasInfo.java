package io.surisoft.capi.lb.schema;

import lombok.Data;

@Data
public class AliasInfo {
    private String alias;
    private String issuerDN;
    private String subjectDN;
    private String additionalInfo;
}
