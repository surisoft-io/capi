package io.surisoft.capi.lb.schema;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(hidden = true)
public class AliasInfo {
    private String alias;
    private String issuerDN;
    private String subjectDN;
    private String additionalInfo;
}
