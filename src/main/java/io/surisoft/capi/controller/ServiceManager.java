package io.surisoft.capi.controller;

import io.surisoft.capi.processor.MetricsProcessor;
import io.surisoft.capi.schema.*;
import io.surisoft.capi.utils.RouteUtils;
import io.surisoft.capi.utils.ServiceUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.camel.CamelContext;
import org.cache2k.Cache;
import org.cache2k.CacheEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/manager")
@Tag(name="CAPI Manager", description = "Management endpoint")
public class ServiceManager {

    @Autowired
    private ServiceUtils serviceUtils;

    @Autowired
    private Cache<String, Service> serviceCache;

    @Autowired
    private Cache<String, StickySession> stickySessionCache;

    @Value("${capi.version}")
    private String capiVersion;

    @Value("${capi.spring.version}")
    private String capiSpringVersion;

    @Autowired
    private CamelContext camelContext;

    @Autowired
    private RouteUtils routeUtils;

    @Autowired
    private MetricsProcessor metricsProcessor;

    @Operation(summary = "Get all cached APIs (Running APIs)")
    @GetMapping(path = "/cached")
    public ResponseEntity<Iterable<Service>> getCachedServices() {
        List<Service> serviceList = new ArrayList<>();
        for (CacheEntry<String, Service> stringServiceCacheEntry : serviceCache.entries()) {
            serviceList.add(stringServiceCacheEntry.getValue());
        }
        return new ResponseEntity<>(serviceList, HttpStatus.OK);
    }

    @Operation(summary = "Get cached Service by ID")
    @GetMapping(path = "/cached/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Service> getCachedService(@PathVariable String id) {
        Service service = serviceCache.peek(id);
        if(service != null) {
            return new ResponseEntity<>(service, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @Operation(summary = "Get CAPI General Server Info")
    @GetMapping(path = "/info", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CapiInfo> getInfo() {
        CapiInfo capiInfo = new CapiInfo();
        capiInfo.setUptime(camelContext.getUptime());
        capiInfo.setCamelVersion(camelContext.getVersion());
        capiInfo.setStartTimestamp(camelContext.getStartDate());
        capiInfo.setTotalRoutes(camelContext.getRoutesSize());
        capiInfo.setCapiVersion(capiVersion);
        capiInfo.setCapiStringVersion(capiSpringVersion);
    return new ResponseEntity<>(capiInfo, HttpStatus.OK);
    }

    @Operation(summary = "Get Running Routes Statistics")
    @GetMapping(path = "/stats/routes", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<RouteDetailsEndpointInfo>> getAllRoutesInfo() {
        List<RouteDetailsEndpointInfo> detailInfoList = new ArrayList<>();
        List<RouteEndpointInfo> routeEndpointInfoList = camelContext.getRoutes().stream()
                                                                .map(RouteEndpointInfo::new)
                                                                .toList();
        for(RouteEndpointInfo routeEndpointInfo : routeEndpointInfoList) {
            if(routeEndpointInfo.getId().startsWith("rd_"))  {
                detailInfoList.add(new RouteDetailsEndpointInfo(camelContext, camelContext.getRoute(routeEndpointInfo.getId())));
            }
        }
        for(RouteDetailsEndpointInfo detailsEndpointInfo : detailInfoList) {
            detailsEndpointInfo.setId(detailsEndpointInfo.getId().replaceAll("rd_", ""));
        }
        return new ResponseEntity<>(detailInfoList, HttpStatus.OK);
    }

    @Operation(summary = "Get all cached Sticky Sessions")
    @GetMapping(path = "/sticky-session")
    public ResponseEntity<Iterable<StickySession>> getCachedStickySession() {
        List<StickySession> stickySessionList = new ArrayList<>();
        for (CacheEntry<String, StickySession> stringServiceCacheEntry : stickySessionCache.entries()) {
            stickySessionList.add(stringServiceCacheEntry.getValue());
        }
        return new ResponseEntity<>(stickySessionList, HttpStatus.OK);
    }
}