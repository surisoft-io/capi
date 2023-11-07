package io.surisoft.capi.builder;

import io.surisoft.capi.schema.Service;
import io.surisoft.capi.utils.Constants;
import io.surisoft.capi.utils.RouteUtils;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestDefinition;

public class RestDefinitionProcessor extends RouteBuilder {
    private final RouteUtils routeUtils;
    private final Service service;
    private final String routeId;

    public RestDefinitionProcessor(CamelContext camelContext, Service service, RouteUtils routeUtils, String routeId) {
        super(camelContext);
        this.service = service;
        this.routeUtils = routeUtils;
        this.routeId = routeId;
    }

    @Override
    public void configure() {
        String restRouteId = Constants.CAMEL_REST_PREFIX + routeId;
        RestDefinition restDefinition = getRestDefinition(service);
        if(restDefinition != null) {
            restDefinition.to(Constants.CAMEL_DIRECT + routeId);
            restDefinition.id(restRouteId);
            routeUtils.registerMetric(restRouteId);
        } else {
            log.warn("Bad definition for service name: {}, please make sure the service context does not contain colons", service.getContext());
        }
    }

    private RestDefinition getRestDefinition(Service service) {
        RestDefinition restDefinition;
        service.setMatchOnUriPrefix(true);

        switch (routeUtils.getMethodFromRouteId(routeId)) {
            case "get" -> restDefinition = rest().get(routeUtils.buildFrom(service)
                    + Constants.MATCH_ON_URI_PREFIX
                    + service.isMatchOnUriPrefix());
            case "post" -> restDefinition = rest().post(routeUtils.buildFrom(service)
                    + Constants.MATCH_ON_URI_PREFIX
                    + service.isMatchOnUriPrefix());
            case "put" -> restDefinition = rest().put(routeUtils.buildFrom(service)
                    + Constants.MATCH_ON_URI_PREFIX
                    + service.isMatchOnUriPrefix());
            case "delete" -> restDefinition = rest().delete(routeUtils.buildFrom(service)
                    + Constants.MATCH_ON_URI_PREFIX
                    + service.isMatchOnUriPrefix());
            case "patch" -> restDefinition = rest().patch(routeUtils.buildFrom(service)
                    + Constants.MATCH_ON_URI_PREFIX
                    + service.isMatchOnUriPrefix());
            default -> {
                return null;
            }
        }
        return restDefinition;
    }
}