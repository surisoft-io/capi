package io.surisoft.capi.lb.schema;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OIDCClientTest {

    private OIDCClient oidcClient;

    @BeforeEach
    void setUp() {
        oidcClient = new OIDCClient();
    }

    @Test
    void testName() {
        oidcClient.setName("name");
        assertEquals("name", oidcClient.getName());
    }

    @Test
    void testClientId() {
        oidcClient.setClientId("clientId");
        assertEquals("clientId", oidcClient.getClientId());
    }

    @Test
    void testSecret() {
        oidcClient.setSecret("secret");
        assertEquals("secret", oidcClient.getSecret());
    }

    @Test
    void testServiceAccountsEnabled() {
        oidcClient.setServiceAccountsEnabled(true);
        assertTrue(oidcClient.isServiceAccountsEnabled());
    }
}