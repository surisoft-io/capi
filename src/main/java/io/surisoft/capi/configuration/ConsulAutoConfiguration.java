package io.surisoft.capi.configuration;

import io.surisoft.capi.processor.ContentTypeValidator;
import io.surisoft.capi.processor.ThrottleProcessor;
import io.surisoft.capi.processor.MetricsProcessor;
import io.surisoft.capi.schema.SSEClient;
import io.surisoft.capi.schema.Service;
import io.surisoft.capi.schema.WebsocketClient;
import io.surisoft.capi.service.ConsulNodeDiscovery;
import io.surisoft.capi.service.OpaService;
import io.surisoft.capi.utils.*;
import org.apache.camel.CamelContext;
import org.cache2k.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Configuration
public class ConsulAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ConsulAutoConfiguration.class);
    private final List<String> capiConsulHosts;
    private final String consulToken;
    private final String capiContext;
    private final boolean reverseProxyEnabled;
    private final String reverseProxyHost;
    private final Map<String, WebsocketClient> websocketClientMap;
    private final Map<String, SSEClient> sseClientMap;
    private final Optional<WebsocketUtils> websocketUtils;
    private final Optional<SSEUtils> sseUtils;
    private final Optional<OpaService> opaService;
    private final String capiNamespace;
    private final boolean strictNamespace;
    private final String capiRunningMode;
    private final ContentTypeValidator contentTypeValidator;
    private final Optional<ThrottleProcessor> globalThrottleProcessor;
    private final Optional<CapiSslContextHolder> capiSslContextHolder;
    private final String serviceMetaExtrasPrefix;

    public ConsulAutoConfiguration(@Value("${capi.consul.hosts}") List<String> capiConsulHosts,
                                   @Value("${capi.consul.token}") String consulToken,
                                   @Value("${camel.servlet.mapping.context-path}") String capiContext,
                                   @Value("${capi.reverse.proxy.enabled}") boolean reverseProxyEnabled,
                                   @Value("${capi.reverse.proxy.host}") String reverseProxyHost,
                                   Map<String, WebsocketClient> websocketClientMap,
                                   Map<String, SSEClient> sseClientMap,
                                   Optional<WebsocketUtils> websocketUtils,
                                   Optional<SSEUtils> sseUtils,
                                   Optional<OpaService> opaService,
                                   @Value("${capi.namespace}") String capiNamespace,
                                   @Value("${capi.strict}") boolean strictNamespace,
                                   @Value("${capi.mode}") String capiRunningMode,
                                   ContentTypeValidator contentTypeValidator,
                                   Optional<ThrottleProcessor> globalThrottleProcessor,
                                   Optional<CapiSslContextHolder> capiSslContextHolder,
                                   @Value("${capi.traces.extra.metadata.prefix}") String serviceMetaExtrasPrefix) {
        this.capiConsulHosts = capiConsulHosts;
        this.consulToken = consulToken;
        this.capiContext = capiContext;
        this.reverseProxyEnabled = reverseProxyEnabled;
        this.reverseProxyHost = reverseProxyHost;
        this.websocketClientMap = websocketClientMap;
        this.sseClientMap = sseClientMap;
        this.websocketUtils = websocketUtils;
        this.sseUtils = sseUtils;
        this.opaService = opaService;
        this.capiNamespace = capiNamespace;
        this.strictNamespace = strictNamespace;
        this.capiRunningMode = capiRunningMode;
        this.contentTypeValidator = contentTypeValidator;
        this.globalThrottleProcessor = globalThrottleProcessor;
        this.capiSslContextHolder = capiSslContextHolder;
        this.serviceMetaExtrasPrefix = serviceMetaExtrasPrefix;
    }

    @Bean(name = "consulNodeDiscovery")
    @ConditionalOnProperty(prefix = "capi.consul.discovery", name = "enabled", havingValue = "true")
    public ConsulNodeDiscovery consulNodeDiscovery(CamelContext camelContext,
                                                   ServiceUtils serviceUtils,
                                                   RouteUtils routeUtils,
                                                   MetricsProcessor metricsProcessor,
                                                   HttpUtils httpUtils,
                                                   Cache<String, Service> serviceCache) {

        ConsulNodeDiscovery consulNodeDiscovery = new ConsulNodeDiscovery(camelContext, serviceUtils, routeUtils, metricsProcessor, serviceCache, websocketClientMap, sseClientMap, contentTypeValidator, globalThrottleProcessor.orElse(null), capiSslContextHolder.orElse(null));
        consulNodeDiscovery.setHttpUtils(httpUtils);

        opaService.ifPresent(consulNodeDiscovery::setOpaService);
        consulNodeDiscovery.setWebsocketUtils(websocketUtils.orElse(null));
        consulNodeDiscovery.setSSEUtils(sseUtils.orElse(null));
        consulNodeDiscovery.setCapiContext(httpUtils.getCapiContext(capiContext));
        consulNodeDiscovery.setConsulHostList(capiConsulHosts);
        if(capiNamespace != null && !capiNamespace.isEmpty()) {
            consulNodeDiscovery.setCapiNamespace(capiNamespace);
            consulNodeDiscovery.setStrictNamespace(strictNamespace);
        }

        if(consulToken != null && !consulToken.isEmpty()) {
            consulNodeDiscovery.setConsulToken(consulToken);
        }

        if(reverseProxyEnabled) {
            consulNodeDiscovery.setReverseProxyHost(reverseProxyHost);
        }

        consulNodeDiscovery.setCapiRunningMode(capiRunningMode);
        if(serviceMetaExtrasPrefix != null && !serviceMetaExtrasPrefix.isEmpty()) {
            consulNodeDiscovery.setServiceMetaExtrasPrefix(serviceMetaExtrasPrefix);
        }

        return consulNodeDiscovery;
    }
}