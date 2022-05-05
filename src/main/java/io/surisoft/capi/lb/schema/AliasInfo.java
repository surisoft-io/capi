package io.surisoft.capi.lb.schema;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(hidden = true)
public class AliasInfo {

    private String alias;
    private String issuerDN;
    private String subjectDN;
    private String additionalInfo;

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getIssuerDN() {
        return issuerDN;
    }

    public void setIssuerDN(String issuerDN) {
        this.issuerDN = issuerDN;
    }

    public String getSubjectDN() {
        return subjectDN;
    }

    public void setSubjectDN(String subjectDN) {
        this.subjectDN = subjectDN;
    }

    public String getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(String additionalInfo) {
        this.additionalInfo = additionalInfo;
    }
}