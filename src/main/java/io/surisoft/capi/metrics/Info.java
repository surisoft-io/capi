package io.surisoft.capi.metrics;

import io.surisoft.capi.schema.CapiInfo;
import org.apache.camel.CamelContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

@Component
@Endpoint(id = "capi")
public class Info {

    @Value("${capi.version}")
    private String capiVersion;
    @Value("${capi.namespace}")
    private String capiNameSpace;
    @Value("${capi.spring.version}")
    private String capiSpringVersion;
    @Value("${oauth2.provider.enabled}")
    private boolean oauth2Enabled;
    @Value("${oauth2.provider.keys}")
    private String oauth2Keys;
    @Value("${opa.enabled}")
    private boolean opaEnabled;
    @Value("${opa.endpoint}")
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

    @Autowired
    private CamelContext camelContext;

    @ReadOperation
    public CapiInfo getInfo() {
        CapiInfo capiInfo = new CapiInfo();
        capiInfo.setUptime(camelContext.getUptime());
        capiInfo.setCamelVersion(camelContext.getVersion());
        capiInfo.setStartTimestamp(camelContext.getStartDate());
        capiInfo.setTotalRoutes(camelContext.getRoutesSize());
        capiInfo.setCapiVersion(capiVersion);
        capiInfo.setCapiStringVersion(capiSpringVersion);
        capiInfo.setCapiNameSpace(capiNameSpace);
        capiInfo.setOpaEnabled(opaEnabled);
        capiInfo.setOpaEndpoint(opaEndpoint);
        capiInfo.setOauth2Enabled(oauth2Enabled);
        capiInfo.setOauth2Endpoint(oauth2Keys);
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