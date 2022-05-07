package io.surisoft.capi.lb.configuration;

import io.surisoft.capi.lb.schema.Api;
import io.surisoft.capi.lb.utils.Constants;
import io.surisoft.capi.lb.utils.RouteUtils;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestDefinition;

public class ConsulRestDefinitionProcessor extends RouteBuilder {

    private RouteUtils routeUtils;
    private Api api;
    private String routeId;

    public ConsulRestDefinitionProcessor(CamelContext camelContext, Api api, RouteUtils routeUtils, String routeId) {
        super(camelContext);
        this.api = api;
        this.routeUtils = routeUtils;
        this.routeId = routeId;
    }

    @Override
    public void configure() {
        String restRouteId = Constants.CAMEL_REST_PREFIX + routeId;
        RestDefinition restDefinition = getRestDefinition(api);
        restDefinition.to(Constants.CAMEL_DIRECT + routeId);
        restDefinition.id(restRouteId);
        routeUtils.registerMetric(restRouteId);
    }

    private RestDefinition getRestDefinition(Api api) {
        RestDefinition restDefinition = null;
        api.setMatchOnUriPrefix(true);

        switch (routeUtils.getMethodFromRouteId(routeId)) {
            case "get":
                restDefinition = rest().get(routeUtils.buildFrom(api)
                        + Constants.MATCH_ON_URI_PREFIX
                        + api.isMatchOnUriPrefix());
                break;
            case "post":
                restDefinition = rest().post(routeUtils.buildFrom(api)
                        + Constants.MATCH_ON_URI_PREFIX
                        + api.isMatchOnUriPrefix());
                break;
            case "put":
                restDefinition = rest().put(routeUtils.buildFrom(api)
                        + Constants.MATCH_ON_URI_PREFIX
                        + api.isMatchOnUriPrefix());
                break;
            case "delete":
                restDefinition = rest().delete(routeUtils.buildFrom(api)
                        + Constants.MATCH_ON_URI_PREFIX
                        + api.isMatchOnUriPrefix());
                break;
            default:
                return null;
        }
        return restDefinition;
    }
}