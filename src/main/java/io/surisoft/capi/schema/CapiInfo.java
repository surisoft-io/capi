package io.surisoft.capi.schema;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.surisoft.capi.configuration.ConsulHosts;

import java.util.Date;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CapiInfo {
    private String javaVersion;
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
    private String capiNameSpace;
    private boolean oauth2Enabled;
    private String oauth2Endpoint;
    private boolean opaEnabled;
    private String opaEndpoint;
    private boolean consulEnabled;
    private List<String> consulHosts;
    private int consulTimerInterval;
    private String routesContextPath;
    private String metricsContextPath;
    private boolean tracesEnabled;
    private String tracesEndpoint;

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

    public String getCapiNameSpace() {
        return capiNameSpace;
    }

    public void setCapiNameSpace(String capiNameSpace) {
        this.capiNameSpace = capiNameSpace;
    }

    public boolean isOauth2Enabled() {
        return oauth2Enabled;
    }

    public void setOauth2Enabled(boolean oauth2Enabled) {
        this.oauth2Enabled = oauth2Enabled;
    }

    public String getOauth2Endpoint() {
        return oauth2Endpoint;
    }

    public void setOauth2Endpoint(String oauth2Endpoint) {
        this.oauth2Endpoint = oauth2Endpoint;
    }

    public boolean isOpaEnabled() {
        return opaEnabled;
    }

    public void setOpaEnabled(boolean opaEnabled) {
        this.opaEnabled = opaEnabled;
    }

    public String getOpaEndpoint() {
        return opaEndpoint;
    }

    public void setOpaEndpoint(String opaEndpoint) {
        this.opaEndpoint = opaEndpoint;
    }

    public boolean isConsulEnabled() {
        return consulEnabled;
    }

    public void setConsulEnabled(boolean consulEnabled) {
        this.consulEnabled = consulEnabled;
    }

    public List<String> getConsulHosts() {
        return consulHosts;
    }

    public void setConsulHosts(List<String> consulHosts) {
        this.consulHosts = consulHosts;
    }

    public int getConsulTimerInterval() {
        return consulTimerInterval;
    }

    public void setConsulTimerInterval(int consulTimerInterval) {
        this.consulTimerInterval = consulTimerInterval;
    }

    public String getRoutesContextPath() {
        return routesContextPath;
    }

    public void setRoutesContextPath(String routesContextPath) {
        this.routesContextPath = routesContextPath;
    }

    public String getMetricsContextPath() {
        return metricsContextPath;
    }

    public void setMetricsContextPath(String metricsContextPath) {
        this.metricsContextPath = metricsContextPath;
    }

    public boolean isTracesEnabled() {
        return tracesEnabled;
    }

    public void setTracesEnabled(boolean tracesEnabled) {
        this.tracesEnabled = tracesEnabled;
    }

    public String getTracesEndpoint() {
        return tracesEndpoint;
    }

    public void setTracesEndpoint(String tracesEndpoint) {
        this.tracesEndpoint = tracesEndpoint;
    }

    public String getJavaVersion() {
        return javaVersion;
    }

    public void setJavaVersion(String javaVersion) {
        this.javaVersion = javaVersion;
    }
}