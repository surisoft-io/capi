package io.surisoft.capi.lb.service;

import io.surisoft.capi.lb.cache.StickySessionCacheManager;
import io.surisoft.capi.lb.processor.MetricsProcessor;
import io.surisoft.capi.lb.repository.ApiRepository;
import io.surisoft.capi.lb.schema.Api;
import io.surisoft.capi.lb.utils.ApiUtils;
import io.surisoft.capi.lb.utils.RouteUtils;
import org.apache.camel.CamelContext;
import org.cache2k.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public class DBNodeDiscovery {

    private static final Logger log = LoggerFactory.getLogger(DBNodeDiscovery.class);

    private final ApiUtils apiUtils;
    private final RouteUtils routeUtils;
    private final MetricsProcessor metricsProcessor;
    private final StickySessionCacheManager stickySessionCacheManager;
    private String capiContext;
    private ApiRepository apiRepository;
    private Cache<String, Api> apiCache;

    private final CamelContext camelContext;

    public DBNodeDiscovery(CamelContext camelContext, ApiUtils apiUtils, RouteUtils routeUtils, MetricsProcessor metricsProcessor, StickySessionCacheManager stickySessionCacheManager, Cache<String, Api> apiCache) {
        this.apiUtils = apiUtils;
        this.routeUtils = routeUtils;
        this.camelContext = camelContext;
        this.stickySessionCacheManager = stickySessionCacheManager;
        this.metricsProcessor = metricsProcessor;
        this.apiCache = apiCache;
    }

    public void processInfo() {
        getAllServices();
    }

    private void getAllServices() {
        Collection<Api> apiList = apiRepository.findAll();
        try {
            apiUtils.removeUnusedApi(camelContext, routeUtils, apiCache, apiList);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        for(Api api : apiList) {
            Api existingApi = apiCache.peek(api.getId());
            if(existingApi == null) {
                routeUtils.createRoute(api, apiCache, camelContext, metricsProcessor, stickySessionCacheManager, capiContext, null);
            } else {
                apiUtils.updateExistingApi(existingApi, api, apiCache, routeUtils, metricsProcessor, camelContext, stickySessionCacheManager, capiContext, null);
            }

        }
    }

    public void setApiRepository(ApiRepository apiRepository) {
        this.apiRepository = apiRepository;
    }

    public void setCapiContext(String capiContext) {
        this.capiContext = capiContext;
    }
}