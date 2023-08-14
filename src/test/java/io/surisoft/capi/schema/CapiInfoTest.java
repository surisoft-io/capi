package io.surisoft.capi.schema;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class CapiInfoTest {

    private CapiInfo capiInfo;

    @BeforeEach
    void setUp() {
        capiInfo = new CapiInfo();
    }

    @Test
    void testCapiVersion() {
        capiInfo.setCapiVersion("1");
        assertEquals("1", capiInfo.getCapiVersion());
    }

    @Test
    void testCapiSpringVersion() {
        capiInfo.setCapiSpringVersion("3");
        assertEquals("3", capiInfo.getCapiSpringVersion());
    }

    @Test
    void testCamelVersion() {
        capiInfo.setCamelVersion("4");
        assertEquals("4", capiInfo.getCamelVersion());
    }

    @Test
    void testStartTimestamp() {
        Date date = Calendar.getInstance().getTime();
        capiInfo.setStartTimestamp(date);
        assertEquals(date, capiInfo.getStartTimestamp());
    }

    @Test
    void testTotalRoutes() {
        capiInfo.setTotalRoutes(1);
        assertEquals(1, capiInfo.getTotalRoutes());
    }

    @Test
    void testExchangesTotal() {
        capiInfo.setExchangesTotal(1);
        assertEquals(1, capiInfo.getExchangesTotal());
    }

    @Test
    void testExchangesCompleted() {
        capiInfo.setExchangesCompleted(1);
        assertEquals(1, capiInfo.getExchangesCompleted());
    }

    @Test
    void testStartedRoutes() {
        capiInfo.setStartedRoutes(1);
        assertEquals(1, capiInfo.getStartedRoutes());
    }

    @Test
    void testUptime() {
        capiInfo.setUptime("1");
        assertEquals("1", capiInfo.getUptime());
    }

    @Test
    void testStoppedRouteCount() {
        capiInfo.setStoppedRouteCount(1);
        assertEquals(1, capiInfo.getStoppedRouteCount());
    }

    @Test
    void testRemovedRouteCount() {
        capiInfo.setRemovedRouteCount(1);
        assertEquals(1, capiInfo.getRemovedRouteCount());
    }

    @Test
    void testFailedExchangeCount() {
        capiInfo.setFailedExchangeCount(1);
        assertEquals(1, capiInfo.getFailedExchangeCount());
    }
}