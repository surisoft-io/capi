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

    @Value("${capi.spring.version}")
    private String capiSpringVersion;

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
        return capiInfo;
    }
}