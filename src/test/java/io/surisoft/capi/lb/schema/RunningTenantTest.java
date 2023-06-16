package io.surisoft.capi.lb.schema;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RunningTenantTest {

    private RunningTenant runningTenant;

    @BeforeEach
    void setUp() {
        runningTenant = new RunningTenant("ten1", 1);
    }

    @Test
    void testGets() {
        assertEquals("ten1", runningTenant.getTenant());
        assertEquals(1, runningTenant.getNodeIndex());
    }

    @Test
    void testSets() {
        runningTenant.setTenant("ten2");
        runningTenant.setNodeIndex(2);
        assertEquals("ten2", runningTenant.getTenant());
        assertEquals(2, runningTenant.getNodeIndex());
    }
}