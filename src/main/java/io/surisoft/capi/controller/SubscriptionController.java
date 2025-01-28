package io.surisoft.capi.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.surisoft.capi.exception.AuthorizationException;
import io.surisoft.capi.schema.CapiRestError;
import io.surisoft.capi.schema.ConsulKeyStoreEntry;
import io.surisoft.capi.schema.Service;
import io.surisoft.capi.schema.SubscriptionGroup;
import io.surisoft.capi.utils.Constants;
import io.surisoft.capi.utils.HttpUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.cache2k.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@RestController
@RequestMapping("/subscription")
public class SubscriptionController {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionController.class);
    private final RestTemplate restTemplate;
    private final HttpUtils httpUtils;
    private final Cache<String, Service> serviceCache;
    private final boolean consulKvStoreEnabled;
    private final boolean oauth2ProviderEnabled;
    private final String consulKvStoreHost;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SubscriptionController(RestTemplate restTemplate,
                                  HttpUtils httpUtils,
                                  Cache<String, Service> serviceCache,
                                  @Value("${capi.consul.kv.enabled}") boolean consulKvStoreEnabled,
                                  @Value("${capi.consul.kv.host}") String consulKvStoreHost,
                                  @Value("${capi.oauth2.provider.enabled}") boolean oauth2ProviderEnabled) {
        this.restTemplate = restTemplate;
        this.httpUtils = httpUtils;
        this.serviceCache = serviceCache;
        this.consulKvStoreEnabled = consulKvStoreEnabled;
        this.oauth2ProviderEnabled = oauth2ProviderEnabled;
        this.consulKvStoreHost = consulKvStoreHost;
    }

    @PutMapping("/{env}/{service}/{group}")
    public ResponseEntity<CapiRestError> putSubscriptionGroupForAGivenService(@PathVariable String env, @PathVariable String service, @PathVariable String group, HttpServletRequest request) {

        String serviceId = env + ":" + service;
        Service cachedService = serviceCache.get(serviceId);
        try {
            CapiRestError capiRestError = isCallAuthorized(request, cachedService);
            if(capiRestError.getErrorCode() != HttpStatus.OK.value()) {
                return new ResponseEntity<>(capiRestError, HttpStatus.valueOf(capiRestError.getErrorCode()));
            }

            Set<String> groups = new HashSet<>();
            groups.add(group);

            SubscriptionGroup subscriptionGroup = getConsulSubscriptionGroup();
            if(subscriptionGroup == null) {
                subscriptionGroup = createEmptySubscriptionGroup();
            }

            if(subscriptionGroup.getServices().containsKey(serviceId)) {
                subscriptionGroup.getServices().get(serviceId).addAll(groups);
            } else {
                subscriptionGroup.getServices().put(serviceId, groups);
            }

            HttpEntity<String> consulRequest = new HttpEntity<>(objectMapper.writeValueAsString(subscriptionGroup));
            restTemplate.put(consulKvStoreHost + Constants.CONSUL_KV_STORE_API + Constants.CONSUL_SUBSCRIPTION_GROUP_KEY, consulRequest);

            return new ResponseEntity<>(HttpStatus.CREATED);

        } catch (AuthorizationException | JsonProcessingException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/{env}/{service}/{group}")
    public ResponseEntity<String> deleteSubscriptionGroupForAGivenService(@PathVariable String env, @PathVariable String service, @PathVariable String group, HttpServletRequest request) {
        return null;
    }

    @GetMapping("/{env}/{service}")
    public ResponseEntity<Object> getSubscriptionsForAGivenService(@PathVariable String env, @PathVariable String service, HttpServletRequest request) {

        String serviceId = env + ":"+ service;
        try {

            Service cachedService = serviceCache.get(serviceId);
            if(cachedService == null) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            CapiRestError capiRestError = isCallAuthorized(request, cachedService);
            if(capiRestError.getErrorCode() != HttpStatus.OK.value()) {
                return new ResponseEntity<>(capiRestError, HttpStatus.valueOf(capiRestError.getErrorCode()));
            }

            Set<String> subscriptionGroups = getConsulSubscriptionGroupsForAService(cachedService);
            if(subscriptionGroups == null) {
                if(cachedService.getServiceMeta().getSubscriptionGroup() == null) {
                    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                } else {
                    subscriptionGroups = httpUtils.stringToSet(cachedService.getServiceMeta().getSubscriptionGroup());
                }
            } else {
                subscriptionGroups.add(cachedService.getServiceMeta().getSubscriptionGroup());
            }
            return new ResponseEntity<>(subscriptionGroups, HttpStatus.OK);
        } catch (AuthorizationException | JsonProcessingException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    private Set<String> getConsulSubscriptionGroupsForAService(Service service) throws JsonProcessingException {
        ResponseEntity<ConsulKeyStoreEntry[]> consulKeyValueStoreResponse = restTemplate.getForEntity(consulKvStoreHost + Constants.CONSUL_KV_STORE_API + Constants.CONSUL_SUBSCRIPTION_GROUP_KEY, ConsulKeyStoreEntry[].class);
        if(consulKeyValueStoreResponse.getStatusCode().is2xxSuccessful()) {
            ConsulKeyStoreEntry consulKeyValueStore = Objects.requireNonNull(consulKeyValueStoreResponse.getBody())[0];
            SubscriptionGroup subscriptionGroup =  httpUtils.consulKeyValueToList(objectMapper, consulKeyValueStore.getValue());
            if(subscriptionGroup != null && subscriptionGroup.getServices().containsKey(service.getContext())) {
                return subscriptionGroup.getServices().get(service.getContext());
            }
        }
        return null;
    }

    private SubscriptionGroup getConsulSubscriptionGroup() throws JsonProcessingException {
        ResponseEntity<ConsulKeyStoreEntry[]> consulKeyValueStoreResponse = restTemplate.getForEntity(consulKvStoreHost + Constants.CONSUL_KV_STORE_API + Constants.CONSUL_SUBSCRIPTION_GROUP_KEY, ConsulKeyStoreEntry[].class);
        if(consulKeyValueStoreResponse.getStatusCode().is2xxSuccessful()) {
            ConsulKeyStoreEntry consulKeyValueStore = Objects.requireNonNull(consulKeyValueStoreResponse.getBody())[0];
            return httpUtils.consulKeyValueToList(objectMapper, consulKeyValueStore.getValue());
        }
        return null;
    }

    private CapiRestError isCallAuthorized(HttpServletRequest request, Service service) throws AuthorizationException {
        String accessToken = httpUtils.processAuthorizationAccessToken(request);
        CapiRestError capiRestError = new CapiRestError();
        if(service == null) {
            capiRestError.setErrorCode(HttpStatus.BAD_REQUEST.value());
            capiRestError.setErrorMessage("Service does not exist");
            return capiRestError;
        }

        if(service.getServiceMeta() != null && !service.getServiceMeta().isAllowSubscriptions()) {
            capiRestError.setErrorCode(HttpStatus.UNAUTHORIZED.value());
            capiRestError.setErrorMessage("Service does not allow subscriptions");
            return capiRestError;
        }

        if(accessToken == null) {
            capiRestError.setErrorCode(HttpStatus.BAD_REQUEST.value());
            capiRestError.setErrorMessage("Missing Authorization token");
            return capiRestError;
        }

        if(!consulKvStoreEnabled) {
            capiRestError.setErrorCode(HttpStatus.NOT_IMPLEMENTED.value());
            capiRestError.setErrorMessage("There is no active key store.");
            return capiRestError;
        }

        if(!oauth2ProviderEnabled) {
            capiRestError.setErrorCode(HttpStatus.NOT_IMPLEMENTED.value());
            capiRestError.setErrorMessage("There is no active authorization provider.");
            return capiRestError;
        }

        if(!httpUtils.isAuthorized(accessToken)) {
            capiRestError.setErrorCode(HttpStatus.UNAUTHORIZED.value());
            capiRestError.setErrorMessage("Invalid Authorization");
            return capiRestError;
        }

        capiRestError.setErrorCode(HttpStatus.OK.value());
        return capiRestError;
    }

    private SubscriptionGroup createEmptySubscriptionGroup() {
        SubscriptionGroup subscriptionGroup = new SubscriptionGroup();
        Map<String, Set<String>> services = new HashMap<>();
        subscriptionGroup.setServices(services);
        return subscriptionGroup;
    }
}