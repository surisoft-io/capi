package io.surisoft.capi.lb.configuration;

import io.surisoft.capi.lb.cache.ConsulDiscoveryCacheManager;
import io.surisoft.capi.lb.cache.RunningApiManager;
import io.surisoft.capi.lb.processor.ConsulNodeDiscovery;
import io.surisoft.capi.lb.repository.ApiRepository;
import io.surisoft.capi.lb.utils.ApiUtils;
import io.surisoft.capi.lb.utils.RouteUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class ConsulAutoConfiguration {

    @Value("${capi.consul.discovery.enabled}")
    private boolean consulEnabled;

    @Value("${capi.consul.discovery.timer.interval}")
    private int consulTimerInterval;

    @Value("${capi.consul.host}")
    private String capiConsulHost;

    @Autowired
    private ConsulDiscoveryCacheManager consulDiscoveryCacheManager;

    @Autowired
    private ApiUtils apiUtils;

    //@Autowired
    private ApiRepository apiRepository;

    @Autowired
    private RouteUtils routeUtils;

    @Autowired
    private RunningApiManager runningApiManager;

    @Bean(name = "consulNodeDiscovery")
    public ConsulNodeDiscovery consulNodeDiscovery() {
        return new ConsulNodeDiscovery(capiConsulHost, apiUtils, apiRepository, routeUtils, runningApiManager);
    }

    @Bean
    public RouteBuilder routeBuilder() {
        log.debug("Creating Capi Consul Discovery");
        if(consulDiscoveryCacheManager.getLocalMemberID().equals(consulDiscoveryCacheManager.getConsulWorkerNode().getMember()) && consulEnabled) {
            return new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                   from("timer:consul-inspect?period=" + consulTimerInterval + "s")
                                .to("bean:consulNodeDiscovery?method=processInfo")
                                .routeId("consul-discovery-service");
                }
            };
        } else {
            return null;
        }
    }
}
