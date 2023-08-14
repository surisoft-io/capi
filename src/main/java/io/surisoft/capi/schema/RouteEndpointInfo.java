package io.surisoft.capi.schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.apache.camel.Route;
import org.apache.camel.StatefulService;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@JsonPropertyOrder({"id", "group", "description", "uptime", "uptimeMillis"})
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class RouteEndpointInfo {
    private String id;
    private String group;
    private Map<String, Object> properties;
    private String description;
    private String uptime;
    private long uptimeMillis;
    private String status;

    public RouteEndpointInfo(Route route) {
        this.id = route.getId();
        this.group = route.getGroup();
        this.description = route.getDescription();
        this.uptime = route.getUptime();
        this.uptimeMillis = route.getUptimeMillis();

        if (route.getProperties() != null) {
            this.properties = new HashMap<>(route.getProperties());
        } else {
            this.properties = Collections.emptyMap();
        }

        if (route instanceof StatefulService) {
            this.status = ((StatefulService) route).getStatus().name();
        } else {
            this.status = null;
        }
    }

    public String getId() {
        return id;
    }

    public String getGroup() {
        return group;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public String getDescription() {
        return description;
    }

    public String getUptime() {
        return uptime;
    }

    public long getUptimeMillis() {
        return uptimeMillis;
    }

    public String getStatus() {
        return status;
    }
}
