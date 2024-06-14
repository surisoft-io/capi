package io.surisoft.capi.metrics;

import io.surisoft.capi.schema.CapiInfo;
import org.apache.camel.CamelContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Endpoint(id = "capi")
public class Info {

    private final List<String> oauth2Keys;
    private final CamelContext camelContext;

    public Info(List<String> oauth2Keys, CamelContext camelContext) {
        this.oauth2Keys = oauth2Keys;
        this.camelContext = camelContext;
    }

    @Value("${capi.version}")
    private String capiVersion;
    @Value("${capi.namespace}")
    private String capiNameSpace;
    @Value("${capi.spring.version}")
    private String capiSpringVersion;
    @Value("${capi.oauth2.provider.enabled}")
    private boolean oauth2Enabled;

    @Value("${capi.opa.enabled}")
    private boolean opaEnabled;
    @Value("${capi.opa.endpoint}")
    private String opaEndpoint;
    @Value("${capi.consul.discovery.enabled}")
    private boolean consulEnabled;
    @Value("${capi.consul.hosts}")
    private String consulEndpoint;
    @Value("${capi.consul.discovery.timer.interval}")
    private int consulTimerInterval;
    @Value("${camel.servlet.mapping.context-path}")
    private String routeContextPath;
    @Value("${management.endpoints.web.base-path}")
    private String metricsContextPath;
    @Value("${capi.traces.enabled}")
    private boolean tracesEnabled;
    @Value("${capi.traces.endpoint}")
    private String tracesEndpoint;


    @ReadOperation
    public CapiInfo getInfo() {
        CapiInfo capiInfo = new CapiInfo();
        capiInfo.setUptime(camelContext.getUptime().toString());
        capiInfo.setCamelVersion(camelContext.getVersion());
        capiInfo.setTotalRoutes(camelContext.getRoutesSize());
        capiInfo.setCapiVersion(capiVersion);
        capiInfo.setCapiStringVersion(capiSpringVersion);
        capiInfo.setCapiNameSpace(capiNameSpace);
        capiInfo.setOpaEnabled(opaEnabled);
        capiInfo.setOpaEndpoint(opaEndpoint);
        capiInfo.setOauth2Enabled(oauth2Enabled);
        capiInfo.setOauth2Endpoint(oauth2Keys.stream().map(String::valueOf).collect(Collectors.joining(",")));
        capiInfo.setConsulEnabled(consulEnabled);
        capiInfo.setConsulEndpoint(consulEndpoint);
        capiInfo.setConsulTimerInterval(consulTimerInterval);
        capiInfo.setRoutesContextPath(routeContextPath);
        capiInfo.setMetricsContextPath(metricsContextPath);
        capiInfo.setTracesEnabled(tracesEnabled);
        capiInfo.setTracesEndpoint(tracesEndpoint);
        return capiInfo;
    }
}