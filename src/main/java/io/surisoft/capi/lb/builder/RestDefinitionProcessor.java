package io.surisoft.capi.lb.builder;

import io.surisoft.capi.lb.processor.SessionChecker;
import io.surisoft.capi.lb.processor.TenantAwareLoadBalancer;
import io.surisoft.capi.lb.schema.Api;
import io.surisoft.capi.lb.utils.Constants;
import io.surisoft.capi.lb.utils.RouteUtils;
import org.apache.camel.CamelConfiguration;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.rest.GetDefinition;
import org.apache.camel.model.rest.RestDefinition;
import org.apache.camel.spi.RestConfiguration;
import org.apache.camel.spring.boot.CamelContextConfiguration;

public class RestDefinitionProcessor extends RouteBuilder {

    private final RouteUtils routeUtils;
    private final Api api;
    private final String routeId;

    public RestDefinitionProcessor(CamelContext camelContext, Api api, RouteUtils routeUtils, String routeId) {
        super(camelContext);
        this.api = api;
        this.routeUtils = routeUtils;
        this.routeId = routeId;
    }

    @Override
    public void configure() {
        String restRouteId = Constants.CAMEL_REST_PREFIX + routeId;
        RestDefinition restDefinition = getRestDefinition(api);
        if(restDefinition != null) {
            restDefinition.to(Constants.CAMEL_DIRECT + routeId);
            restDefinition.id(restRouteId);
            routeUtils.registerMetric(restRouteId);
        } else {
            log.warn("Bad definition for service name: {}, please make sure the service context does not contain colons", api.getContext());
        }
    }

    private RestDefinition getRestDefinition(Api api) {
        RestDefinition restDefinition;
        api.setMatchOnUriPrefix(true);

        switch (routeUtils.getMethodFromRouteId(routeId)) {
            case "get" -> restDefinition = rest().get(routeUtils.buildFrom(api)
                    + Constants.MATCH_ON_URI_PREFIX
                    + api.isMatchOnUriPrefix());
            case "post" -> restDefinition = rest().post(routeUtils.buildFrom(api)
                    + Constants.MATCH_ON_URI_PREFIX
                    + api.isMatchOnUriPrefix());
            case "put" -> restDefinition = rest().put(routeUtils.buildFrom(api)
                    + Constants.MATCH_ON_URI_PREFIX
                    + api.isMatchOnUriPrefix());
            case "delete" -> restDefinition = rest().delete(routeUtils.buildFrom(api)
                    + Constants.MATCH_ON_URI_PREFIX
                    + api.isMatchOnUriPrefix());
            case "patch" -> restDefinition = rest().patch(routeUtils.buildFrom(api)
                    + Constants.MATCH_ON_URI_PREFIX
                    + api.isMatchOnUriPrefix());
            default -> {
                return null;
            }
        }
        return restDefinition;
    }
}