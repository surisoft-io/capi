package io.surisoft.capi.controller;

import io.surisoft.capi.repository.ApiRepository;
import io.surisoft.capi.repository.MappingRepository;
import io.surisoft.capi.schema.*;
import io.surisoft.capi.schema.Mapping;
import io.surisoft.capi.utils.ApiUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.camel.CamelContext;
import org.cache2k.Cache;
import org.cache2k.CacheEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/manager")
@Tag(name="CAPI Manager", description = "Management endpoint")
public class ApiManager {

    @Autowired
    private ApiRepository apiRepository;

    @Autowired
    private MappingRepository mappingRepository;

    @Autowired
    private ApiUtils apiUtils;

    @Autowired
    private Cache<String, Api> apiCache;

    //@Autowired
    //private Cache<String, String> startRouteStoppedEventCache;

    //@Autowired
    //private Cache<String, String> startRouteRemovedEventCache;

    //@Autowired
    //private Cache<String, String> startExchangeFailedEventCache;

    @Value("${capi.persistence.enabled}")
    private boolean capiPersistenceEnabled;

    @Value("${capi.version}")
    private String capiVersion;

    @Value("${capi.spring.version}")
    private String capiSpringVersion;

    @Autowired
    private CamelContext camelContext;

    @Operation(summary = "Get all configured APIs (Only if Database persistence is available)")
    @GetMapping(path = "/configured")
    public ResponseEntity<Iterable<Api>> getAllApi() {
        if(capiPersistenceEnabled) {
            return new ResponseEntity<>(apiRepository.findAll(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(null, HttpStatus.NOT_IMPLEMENTED);
        }
    }

    @Operation(summary = "Get all cached APIs (Running APIs)")
    @GetMapping(path = "/cached")
    public ResponseEntity<Iterable<Api>> getCachedApi() {
        List<Api> apiList = new ArrayList<>();
        for (CacheEntry<String, Api> stringApiCacheEntry : apiCache.entries()) {
            apiList.add(stringApiCacheEntry.getValue());
        }
        return new ResponseEntity<>(apiList, HttpStatus.OK);
    }

    @Operation(summary = "Get cached API by Route ID")
    @GetMapping(path = "/cached/{routeId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Api> getCachedApi(@PathVariable String routeId) {
        Api api = apiCache.peek(routeId);
        if(api != null) {
            return new ResponseEntity<>(api, HttpStatus.OK);
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

        //capiInfo.setStoppedRouteCount(startRouteStoppedEventCache.keys().size());
        //capiInfo.setRemovedRouteCount(startRouteRemovedEventCache.keys().size());
        //capiInfo.setFailedExchangeCount(startExchangeFailedEventCache.keys().size());



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

    @Operation(summary = "Register a node (Only if Database persistence is available)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Node added"),
            @ApiResponse(responseCode = "400", description = "Bad request")
    })
    @PostMapping(path="/register/node")
    public ResponseEntity<Api> newNodeMapping(@RequestBody Api api) {
        if(!capiPersistenceEnabled) {
            return new  ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
        }

        if(!isNodeInfoValid(api)) {
           return new  ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        String apiId = apiUtils.getApiId(api);
        Optional<Api> existingApi = apiRepository.findById(apiId);
        if(existingApi.isPresent()) {
            apiUtils.updateExistingApi(existingApi.get(), api, apiRepository);
            return new ResponseEntity<>(api, HttpStatus.OK);
        }

        api.setId(apiId);
        api.setPublished(true);
        apiUtils.applyApiDefaults(api);
        apiRepository.save(api);

        return new ResponseEntity<>(api, HttpStatus.OK);
    }

    @Operation(summary = "Unregister a node, with the option of removing the entire API. (Only if Database persistence is available)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Node/API removed"),
            @ApiResponse(responseCode = "400", description = "Bad request")
    })
    @PostMapping(path="/unregister/node")
    public ResponseEntity<Api> deleteMapping(@RequestBody Api api) {
        if(!capiPersistenceEnabled) {
            return new  ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
        }

        if(!isNodeInfoValid(api)) {
            return new  ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        String apiId = apiUtils.getApiId(api);
        Optional<Api> existingApi = apiRepository.findById(apiId);
        if(existingApi.isEmpty()) {
            return new  ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        if(api.isRemoveMe()) {
            deleteMappingAndUpdate(api, existingApi);
        } else {
            //delete all
            deleteAllRoutes(existingApi);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    private boolean isNodeInfoValid(Api api) {
        return api != null && api.getContext() != null && api.getName() != null && api.getMappingList() != null && !api.getMappingList().isEmpty();
    }

    private void deleteAllRoutes(Optional<Api> existingApi) {
        existingApi.ifPresent(api -> apiRepository.delete(api));
    }

    private void deleteMappingAndUpdate(Api api, Optional<Api> existingApi) {
        Mapping mapping = api.getMappingList().stream().toList().get(0);
        if(existingApi.isPresent()) {
            if(existingApi.get().getMappingList().contains(mapping)) {
                existingApi.get().getMappingList().remove(mapping);
                if(existingApi.get().getMappingList().isEmpty()) {
                    apiRepository.delete(existingApi.get());
                    mappingRepository.delete(mapping);
                } else {
                    updateExistingMapping(existingApi, mapping);
                }
            }
        }
    }

    private void updateExistingMapping(Optional<Api> existingApi, Mapping mapping) {
        if(existingApi.isPresent()) {
            apiRepository.update(existingApi.get());
            mappingRepository.delete(mapping);
        }
    }
}