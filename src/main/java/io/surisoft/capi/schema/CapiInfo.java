package io.surisoft.capi.schema;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Date;
@JsonIgnoreProperties(ignoreUnknown = true)
public class CapiInfo {
    private String capiVersion;
    private String capiSpringVersion;
    private String camelVersion;
    private Date startTimestamp;
    private Integer totalRoutes;
    private Integer exchangesTotal;
    private Integer exchangesCompleted;
    private Integer startedRoutes;
    private String uptime;
    private Integer stoppedRouteCount;
    private Integer removedRouteCount;
    private Integer failedExchangeCount;

    private boolean oauth2Enabled;
    private boolean stickySessionEnabled;
    private boolean grpcEnabled;
    private boolean kafkaEnabled;
    private boolean webSocketEnabled;
    private boolean traceEnabled;
    private boolean gatewayCorsManagementEnabled;
    private String consulHost;

    public String getCapiVersion() {
        return capiVersion;
    }

    public void setCapiVersion(String capiVersion) {
        this.capiVersion = capiVersion;
    }

    public String getCapiSpringVersion() {
        return capiSpringVersion;
    }

    public void setCapiStringVersion(String capiSpringVersion) {
        this.capiSpringVersion = capiSpringVersion;
    }

    public void setCapiSpringVersion(String capiSpringVersion) {
        this.capiSpringVersion = capiSpringVersion;
    }

    public String getCamelVersion() {
        return camelVersion;
    }

    public void setCamelVersion(String camelVersion) {
        this.camelVersion = camelVersion;
    }

    public Date getStartTimestamp() {
        return startTimestamp;
    }

    public void setStartTimestamp(Date startTimestamp) {
        this.startTimestamp = startTimestamp;
    }

    public Integer getTotalRoutes() {
        return totalRoutes;
    }

    public void setTotalRoutes(Integer totalRoutes) {
        this.totalRoutes = totalRoutes;
    }

    public Integer getExchangesTotal() {
        return exchangesTotal;
    }

    public void setExchangesTotal(Integer exchangesTotal) {
        this.exchangesTotal = exchangesTotal;
    }

    public Integer getExchangesCompleted() {
        return exchangesCompleted;
    }

    public void setExchangesCompleted(Integer exchangesCompleted) {
        this.exchangesCompleted = exchangesCompleted;
    }

    public Integer getStartedRoutes() {
        return startedRoutes;
    }

    public void setStartedRoutes(Integer startedRoutes) {
        this.startedRoutes = startedRoutes;
    }

    public String getUptime() {
        return uptime;
    }

    public void setUptime(String uptime) {
        this.uptime = uptime;
    }

    public Integer getStoppedRouteCount() {
        return stoppedRouteCount;
    }

    public void setStoppedRouteCount(Integer stoppedRouteCount) {
        this.stoppedRouteCount = stoppedRouteCount;
    }

    public Integer getRemovedRouteCount() {
        return removedRouteCount;
    }

    public void setRemovedRouteCount(Integer removedRouteCount) {
        this.removedRouteCount = removedRouteCount;
    }

    public Integer getFailedExchangeCount() {
        return failedExchangeCount;
    }

    public void setFailedExchangeCount(Integer failedExchangeCount) {
        this.failedExchangeCount = failedExchangeCount;
    }
}