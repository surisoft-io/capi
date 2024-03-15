package io.surisoft.capi.schema;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AliasInfoTest {


    private AliasInfo aliasInfo;

    @BeforeEach
    void createInstance() {
        aliasInfo = new AliasInfo();
    }

    @Test
    void testAlias() {
        aliasInfo.setAlias("alias");
        assertEquals("alias", aliasInfo.getAlias());
    }

    @Test
    void testIssuerDN() {
        aliasInfo.setIssuerDN("dn");
        assertEquals("dn", aliasInfo.getIssuerDN());
    }

    @Test
    void testSubjectDN() {
        aliasInfo.setSubjectDN("sub");
        assertEquals("sub", aliasInfo.getSubjectDN());
    }

    @Test
    void testAdditionalInfo() {
        aliasInfo.setAdditionalInfo("add");
        assertEquals("add", aliasInfo.getAdditionalInfo());
    }

    @Test
    void testApiId() {
        aliasInfo.setServiceId("api");
        assertEquals("api", aliasInfo.getServiceId());
    }
}