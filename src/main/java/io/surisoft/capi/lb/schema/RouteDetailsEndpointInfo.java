package io.surisoft.capi.lb.schema;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.camel.CamelContext;
import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.Route;
import org.apache.camel.api.management.ManagedCamelContext;
import org.apache.camel.api.management.mbean.ManagedRouteMBean;

public class RouteDetailsEndpointInfo extends RouteEndpointInfo{
    @JsonProperty("details")
    private RouteDetails routeDetails;

    public RouteDetailsEndpointInfo(final CamelContext camelContext, final Route route) {
        super(route);
        if (camelContext.getManagementStrategy().getManagementAgent() != null) {
            ManagedCamelContext mcc = camelContext.getCamelContextExtension().getContextPlugin(ManagedCamelContext.class);
            this.routeDetails = new RouteDetails(mcc.getManagedRoute(route.getId(), ManagedRouteMBean.class));
        }
    }
}