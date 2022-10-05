package io.surisoft.capi.lb.builder;

import io.surisoft.capi.lb.schema.Api;
import io.surisoft.capi.lb.utils.Constants;
import io.surisoft.capi.lb.utils.RouteUtils;
import org.apache.camel.CamelConfiguration;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.GetDefinition;
import org.apache.camel.model.rest.RestDefinition;
import org.apache.camel.spi.RestConfiguration;
import org.apache.camel.spring.boot.CamelContextConfiguration;

public class RestDefinitionProcessor extends RouteBuilder {

    private RouteUtils routeUtils;
    private Api api;
    private String routeId;

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
            log.error("Null Rest Definition for routeId {}", routeId);
        }
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
            case "patch":
                restDefinition = rest().patch(routeUtils.buildFrom(api)
                        + Constants.MATCH_ON_URI_PREFIX
                        + api.isMatchOnUriPrefix());
                break;
            default:
                return null;
        }
        return restDefinition;
    }
}