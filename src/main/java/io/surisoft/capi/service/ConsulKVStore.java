package io.surisoft.capi.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.surisoft.capi.schema.SubscriptionGroup;
import io.surisoft.capi.schema.ConsulKeyStoreEntry;
import io.surisoft.capi.schema.Service;
import io.surisoft.capi.utils.Constants;
import io.surisoft.capi.utils.HttpUtils;
import org.cache2k.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.*;

public class ConsulKVStore {

    private static final Logger log = LoggerFactory.getLogger(ConsulKVStore.class);
    private final RestTemplate restTemplate;
    private final Cache<String, List<String>> corsHeadersCache;
    private final Cache<String, ConsulKeyStoreEntry> consulSubscriptionGroupCache;
    private final Cache<String, Service> serviceCache;
    private final HttpUtils httpUtils;
    private final String consulHost;
    private final String consulKvHost;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ConsulKVStore(RestTemplate restTemplate, Cache<String, List<String>> corsHeadersCache, Cache<String, ConsulKeyStoreEntry> consulSubscriptionGroupCache, Cache<String, Service> serviceCache, HttpUtils httpUtils, String consulHost, String consulKvHost) {
        this.restTemplate = restTemplate;
        this.corsHeadersCache = corsHeadersCache;
        this.consulSubscriptionGroupCache = consulSubscriptionGroupCache;
        this.serviceCache = serviceCache;
        this.httpUtils = httpUtils;
        this.consulHost = consulHost;
        this.consulKvHost = consulKvHost;
    }

    public void process() {
        log.debug("Looking for key values...");
        capiCorsHeadersKVCall();
        syncSubscriptions();
    }

    private void capiCorsHeadersKVCall() {
        List<String> cachedValueAsList = corsHeadersCache.get("capi-cors-headers");
        try {
            ResponseEntity<ConsulKeyStoreEntry[]> consulKeyValueStoreResponse = restTemplate.getForEntity(consulHost + Constants.CONSUL_KV_STORE_API + Constants.CAPI_CORS_HEADERS_CACHE_KEY, ConsulKeyStoreEntry[].class);
            if(consulKeyValueStoreResponse.getStatusCode().is2xxSuccessful()) {
                ConsulKeyStoreEntry consulKeyValueStore = Objects.requireNonNull(consulKeyValueStoreResponse.getBody())[0];
                List<String> consulDecodedValueAsList = consulKeyValueToList(consulKeyValueStore.getValue());
                if(cachedValueAsList != null && !consulDecodedValueAsList.isEmpty() && !cachedValueAsList.isEmpty() && !consulDecodedValueAsList.equals(cachedValueAsList)) {
                    log.debug("Cached Values Are different we need to save them to Consul KV");
                    updateConsulHeaderKey(Constants.CAPI_CORS_HEADERS_CACHE_KEY, String.join(",", cachedValueAsList));
                } else {
                    log.debug("Go to sleep and try later on...");
                }
            }
        } catch(Exception e) {
            log.debug("Cached Values Are different we need to save them to Consul KV");
            assert cachedValueAsList != null;
            updateConsulHeaderKey("capi-cors-headers", String.join(",", cachedValueAsList));
        }
    }

    private void syncSubscriptions() {
        ConsulKeyStoreEntry subscriptionGroupCached = consulSubscriptionGroupCache.get(Constants.CONSUL_SUBSCRIPTION_GROUP_KEY);
        ConsulKeyStoreEntry subscriptionGroupRemote = getRemoteSubscriptions();
        try {
            //Found remote
            if(subscriptionGroupRemote != null ) {
                if(subscriptionGroupCached != null) {
                    if(subscriptionGroupRemote.getModifyIndex() != subscriptionGroupCached.getModifyIndex()) {
                        log.debug("The remote object is different from the local, lets update the local");
                        processSubscriptions(subscriptionGroupRemote, true);
                        consulSubscriptionGroupCache.put(Constants.CONSUL_SUBSCRIPTION_GROUP_KEY, subscriptionGroupRemote);
                    } else {
                        log.debug("The remote object is equal to the local, nothing to do for now.");
                        processSubscriptions(subscriptionGroupCached, false);
                        consulSubscriptionGroupCache.put(Constants.CONSUL_SUBSCRIPTION_GROUP_KEY, subscriptionGroupCached);
                    }
                } else {
                    log.debug("Found remote subscriptions but not local, CAPI will cache the remote for the first time.");
                    processSubscriptions(subscriptionGroupRemote, true);
                    consulSubscriptionGroupCache.put(Constants.CONSUL_SUBSCRIPTION_GROUP_KEY, subscriptionGroupRemote);
                }
            } else {
                log.debug("No remote subscriptions found, so nothing to do for now.");
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private void updateConsulHeaderKey(String key, String value) {
        HttpEntity<String> request = new HttpEntity<>(value);
        restTemplate.put(consulHost + Constants.CONSUL_KV_STORE_API + key, request);
    }

    private List<String> consulKeyValueToList(String encodedValue) {
        String decodedValue = new String(Base64.getDecoder().decode(encodedValue));
        return Arrays.asList(decodedValue.split(",", -1));
    }

    private ConsulKeyStoreEntry getRemoteSubscriptions() {
        if(consulKvHost != null) {
            try {
                ResponseEntity<ConsulKeyStoreEntry[]> consulKeyValueStoreResponse = restTemplate.getForEntity(consulKvHost + Constants.CONSUL_KV_STORE_API + Constants.CONSUL_SUBSCRIPTION_GROUP_KEY, ConsulKeyStoreEntry[].class);
                if (consulKeyValueStoreResponse.getStatusCode().is2xxSuccessful()) {
                    return Objects.requireNonNull(consulKeyValueStoreResponse.getBody())[0];
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
        return null;
    }

    private void processSubscriptions(ConsulKeyStoreEntry subscriptionGroupObject, boolean remoteChanges) throws JsonProcessingException {
        SubscriptionGroup subscriptionGroup = httpUtils.consulKeyValueToList(objectMapper, subscriptionGroupObject.getValue());
        Set<String> mergedServices = subscriptionGroupObject.getServicesProcessed();
        if(mergedServices == null) {
            mergedServices = new HashSet<>();
        }
        for(String key : subscriptionGroup.getServices().keySet()) {
            Service service = serviceCache.get(key);
            if(service != null) {
                Set<String> serviceMetaGroups = httpUtils.subscriptionGroupToList(service.getServiceMeta().getSubscriptionGroup());
                if(remoteChanges) {
                    serviceMetaGroups.addAll(subscriptionGroup.getServices().get(key));
                    service.getServiceMeta().setSubscriptionGroup(String.join(",", serviceMetaGroups));
                    serviceCache.put(key, service);
                    mergedServices.add(key);
                } else {
                    if(!mergedServices.contains(key)) {
                        log.debug("Found a service for processing subscriptions");
                        serviceMetaGroups.addAll(subscriptionGroup.getServices().get(key));
                        service.getServiceMeta().setSubscriptionGroup(String.join(",", serviceMetaGroups));
                        serviceCache.put(key, service);
                        mergedServices.add(key);
                    }
                }
                subscriptionGroupObject.setServicesProcessed(mergedServices);
            }
        }
    }
}