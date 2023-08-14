package io.surisoft.capi.schema;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RouteDetailsTest {
    RouteDetails routeDetails;

    @BeforeEach
    void setUp() {
        routeDetails = new RouteDetails();
    }

    @Test
    void testDeltaProcessingTime() {
        routeDetails.setDeltaProcessingTime(1);
        assertEquals(1, routeDetails.getDeltaProcessingTime());
    }

    @Test
    void testExchangesInflight() {
        routeDetails.setExchangesInflight(1);
        assertEquals(1, routeDetails.getExchangesInflight());
    }

    @Test
    void testExchangesTotal() {
        routeDetails.setExchangesTotal(1);
        assertEquals(1, routeDetails.getExchangesTotal());
    }

    @Test
    void testExternalRedeliveries() {
        routeDetails.setExternalRedeliveries(1);
        assertEquals(1, routeDetails.getExternalRedeliveries());
    }

    @Test
    void testFailuresHandled() {
        routeDetails.setFailuresHandled(1);
        assertEquals(1, routeDetails.getFailuresHandled());
    }

    @Test
    void testFirstExchangeCompletedExchangeId() {
        routeDetails.setFirstExchangeCompletedExchangeId("ID");
        assertEquals("ID", routeDetails.getFirstExchangeCompletedExchangeId());
    }

    @Test
    void testFirstExchangeCompletedTimestamp() {
        Date date = Calendar.getInstance().getTime();
        routeDetails.setFirstExchangeCompletedTimestamp(date);
        assertEquals(date, routeDetails.getFirstExchangeCompletedTimestamp());
    }

    @Test
    void testFirstExchangeFailureExchangeId() {
        routeDetails.setFirstExchangeFailureExchangeId("ID");
        assertEquals("ID", routeDetails.getFirstExchangeFailureExchangeId());
    }

    @Test
    void testFirstExchangeFailureTimestamp() {
        Date date = Calendar.getInstance().getTime();
        routeDetails.setFirstExchangeFailureTimestamp(date);
        assertEquals(date, routeDetails.getFirstExchangeFailureTimestamp());
    }

    @Test
    void testLastExchangeCompletedExchangeId() {
        routeDetails.setLastExchangeCompletedExchangeId("ID");
        assertEquals("ID", routeDetails.getLastExchangeCompletedExchangeId());
    }

    @Test
    void testLastExchangeCompletedTimestamp() {
        Date date = Calendar.getInstance().getTime();
        routeDetails.setLastExchangeCompletedTimestamp(date);
        assertEquals(date, routeDetails.getLastExchangeCompletedTimestamp());
    }

    @Test
    void testLastExchangeFailureExchangeId() {
        routeDetails.setLastExchangeFailureExchangeId("ID");
        assertEquals("ID", routeDetails.getLastExchangeFailureExchangeId());
    }

    @Test
    void testLastExchangeFailureTimestamp() {
        Date date = Calendar.getInstance().getTime();
        routeDetails.setLastExchangeFailureTimestamp(date);
        assertEquals(date, routeDetails.getLastExchangeFailureTimestamp());
    }

   @Test
    void testLastProcessingTime() {
        routeDetails.setLastProcessingTime(1);
        assertEquals(1, routeDetails.getLastProcessingTime());
    }

    @Test
    void getMaxProcessingTime() {
    }

    @Test
    void setMaxProcessingTime() {
    }

    @Test
    void getMeanProcessingTime() {
    }

    @Test
    void setMeanProcessingTime() {
    }

    @Test
    void getMinProcessingTime() {
    }

    @Test
    void setMinProcessingTime() {
    }

    @Test
    void getOldestInflightDuration() {
    }

    @Test
    void setOldestInflightDuration() {
    }

    @Test
    void getOldestInflightExchangeId() {
    }

    @Test
    void setOldestInflightExchangeId() {
    }

    @Test
    void getRedeliveries() {
    }

    @Test
    void setRedeliveries() {
    }

    @Test
    void getTotalProcessingTime() {
    }

    @Test
    void setTotalProcessingTime() {
    }

    @Test
    void isHasRouteController() {
    }

    @Test
    void setHasRouteController() {
    }
}