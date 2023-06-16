package io.surisoft.capi.lb.schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.camel.api.management.mbean.ManagedRouteMBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class RouteDetails {
    private static final Logger LOG = LoggerFactory.getLogger(RouteDetails.class);
    private long deltaProcessingTime;
    private long exchangesInflight;
    private long exchangesTotal;
    private long externalRedeliveries;
    private long failuresHandled;
    private String firstExchangeCompletedExchangeId;
    private Date firstExchangeCompletedTimestamp;
    private String firstExchangeFailureExchangeId;
    private Date firstExchangeFailureTimestamp;
    private String lastExchangeCompletedExchangeId;
    private Date lastExchangeCompletedTimestamp;
    private String lastExchangeFailureExchangeId;
    private Date lastExchangeFailureTimestamp;
    private long lastProcessingTime;
    private String load01;
    private String load05;
    private String load15;
    private long maxProcessingTime;
    private long meanProcessingTime;
    private long minProcessingTime;
    private Long oldestInflightDuration;
    private String oldestInflightExchangeId;
    private long redeliveries;
    private long totalProcessingTime;
    private boolean hasRouteController;

    public RouteDetails() {
        LOG.info("Used for unit test");
    }

    public RouteDetails(ManagedRouteMBean managedRoute) {
        try {
            this.deltaProcessingTime = managedRoute.getDeltaProcessingTime();
            this.exchangesInflight = managedRoute.getExchangesInflight();
            this.exchangesTotal = managedRoute.getExchangesTotal();
            this.externalRedeliveries = managedRoute.getExternalRedeliveries();
            this.failuresHandled = managedRoute.getFailuresHandled();
            this.firstExchangeCompletedExchangeId = managedRoute.getFirstExchangeCompletedExchangeId();
            this.firstExchangeCompletedTimestamp = managedRoute.getFirstExchangeCompletedTimestamp();
            this.firstExchangeFailureExchangeId = managedRoute.getFirstExchangeFailureExchangeId();
            this.firstExchangeFailureTimestamp = managedRoute.getFirstExchangeFailureTimestamp();
            this.lastExchangeCompletedExchangeId = managedRoute.getLastExchangeCompletedExchangeId();
            this.lastExchangeCompletedTimestamp = managedRoute.getLastExchangeCompletedTimestamp();
            this.lastExchangeFailureExchangeId = managedRoute.getLastExchangeFailureExchangeId();
            this.lastExchangeFailureTimestamp = managedRoute.getLastExchangeFailureTimestamp();
            this.lastProcessingTime = managedRoute.getLastProcessingTime();
            this.load01 = managedRoute.getLoad01();
            this.load05 = managedRoute.getLoad05();
            this.load15 = managedRoute.getLoad15();
            this.maxProcessingTime = managedRoute.getMaxProcessingTime();
            this.meanProcessingTime = managedRoute.getMeanProcessingTime();
            this.minProcessingTime = managedRoute.getMinProcessingTime();
            this.oldestInflightDuration = managedRoute.getOldestInflightDuration();
            this.oldestInflightExchangeId = managedRoute.getOldestInflightExchangeId();
            this.redeliveries = managedRoute.getRedeliveries();
            this.totalProcessingTime = managedRoute.getTotalProcessingTime();
            this.hasRouteController = managedRoute.getHasRouteController();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    public long getDeltaProcessingTime() {
        return deltaProcessingTime;
    }

    public void setDeltaProcessingTime(long deltaProcessingTime) {
        this.deltaProcessingTime = deltaProcessingTime;
    }

    public long getExchangesInflight() {
        return exchangesInflight;
    }

    public void setExchangesInflight(long exchangesInflight) {
        this.exchangesInflight = exchangesInflight;
    }

    public long getExchangesTotal() {
        return exchangesTotal;
    }

    public void setExchangesTotal(long exchangesTotal) {
        this.exchangesTotal = exchangesTotal;
    }

    public long getExternalRedeliveries() {
        return externalRedeliveries;
    }

    public void setExternalRedeliveries(long externalRedeliveries) {
        this.externalRedeliveries = externalRedeliveries;
    }

    public long getFailuresHandled() {
        return failuresHandled;
    }

    public void setFailuresHandled(long failuresHandled) {
        this.failuresHandled = failuresHandled;
    }

    public String getFirstExchangeCompletedExchangeId() {
        return firstExchangeCompletedExchangeId;
    }

    public void setFirstExchangeCompletedExchangeId(String firstExchangeCompletedExchangeId) {
        this.firstExchangeCompletedExchangeId = firstExchangeCompletedExchangeId;
    }

    public Date getFirstExchangeCompletedTimestamp() {
        return firstExchangeCompletedTimestamp;
    }

    public void setFirstExchangeCompletedTimestamp(Date firstExchangeCompletedTimestamp) {
        this.firstExchangeCompletedTimestamp = firstExchangeCompletedTimestamp;
    }

    public String getFirstExchangeFailureExchangeId() {
        return firstExchangeFailureExchangeId;
    }

    public void setFirstExchangeFailureExchangeId(String firstExchangeFailureExchangeId) {
        this.firstExchangeFailureExchangeId = firstExchangeFailureExchangeId;
    }

    public Date getFirstExchangeFailureTimestamp() {
        return firstExchangeFailureTimestamp;
    }

    public void setFirstExchangeFailureTimestamp(Date firstExchangeFailureTimestamp) {
        this.firstExchangeFailureTimestamp = firstExchangeFailureTimestamp;
    }

    public String getLastExchangeCompletedExchangeId() {
        return lastExchangeCompletedExchangeId;
    }

    public void setLastExchangeCompletedExchangeId(String lastExchangeCompletedExchangeId) {
        this.lastExchangeCompletedExchangeId = lastExchangeCompletedExchangeId;
    }

    public Date getLastExchangeCompletedTimestamp() {
        return lastExchangeCompletedTimestamp;
    }

    public void setLastExchangeCompletedTimestamp(Date lastExchangeCompletedTimestamp) {
        this.lastExchangeCompletedTimestamp = lastExchangeCompletedTimestamp;
    }

    public String getLastExchangeFailureExchangeId() {
        return lastExchangeFailureExchangeId;
    }

    public void setLastExchangeFailureExchangeId(String lastExchangeFailureExchangeId) {
        this.lastExchangeFailureExchangeId = lastExchangeFailureExchangeId;
    }

    public Date getLastExchangeFailureTimestamp() {
        return lastExchangeFailureTimestamp;
    }

    public void setLastExchangeFailureTimestamp(Date lastExchangeFailureTimestamp) {
        this.lastExchangeFailureTimestamp = lastExchangeFailureTimestamp;
    }

    public long getLastProcessingTime() {
        return lastProcessingTime;
    }

    public void setLastProcessingTime(long lastProcessingTime) {
        this.lastProcessingTime = lastProcessingTime;
    }

    public String getLoad01() {
        return load01;
    }

    public void setLoad01(String load01) {
        this.load01 = load01;
    }

    public String getLoad05() {
        return load05;
    }

    public void setLoad05(String load05) {
        this.load05 = load05;
    }

    public String getLoad15() {
        return load15;
    }

    public void setLoad15(String load15) {
        this.load15 = load15;
    }

    public long getMaxProcessingTime() {
        return maxProcessingTime;
    }

    public void setMaxProcessingTime(long maxProcessingTime) {
        this.maxProcessingTime = maxProcessingTime;
    }

    public long getMeanProcessingTime() {
        return meanProcessingTime;
    }

    public void setMeanProcessingTime(long meanProcessingTime) {
        this.meanProcessingTime = meanProcessingTime;
    }

    public long getMinProcessingTime() {
        return minProcessingTime;
    }

    public void setMinProcessingTime(long minProcessingTime) {
        this.minProcessingTime = minProcessingTime;
    }

    public Long getOldestInflightDuration() {
        return oldestInflightDuration;
    }

    public void setOldestInflightDuration(Long oldestInflightDuration) {
        this.oldestInflightDuration = oldestInflightDuration;
    }

    public String getOldestInflightExchangeId() {
        return oldestInflightExchangeId;
    }

    public void setOldestInflightExchangeId(String oldestInflightExchangeId) {
        this.oldestInflightExchangeId = oldestInflightExchangeId;
    }

    public long getRedeliveries() {
        return redeliveries;
    }

    public void setRedeliveries(long redeliveries) {
        this.redeliveries = redeliveries;
    }

    public long getTotalProcessingTime() {
        return totalProcessingTime;
    }

    public void setTotalProcessingTime(long totalProcessingTime) {
        this.totalProcessingTime = totalProcessingTime;
    }

    public boolean isHasRouteController() {
        return hasRouteController;
    }

    public void setHasRouteController(boolean hasRouteController) {
        this.hasRouteController = hasRouteController;
    }
}