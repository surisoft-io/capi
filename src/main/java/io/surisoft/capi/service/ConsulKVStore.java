package io.surisoft.capi.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.surisoft.capi.configuration.CapiSslContextHolder;
import io.surisoft.capi.schema.ConsulKeyStoreEntry;
import io.surisoft.capi.utils.Constants;
import io.surisoft.capi.utils.RouteUtils;
import org.apache.camel.CamelContext;
import org.apache.camel.component.http.HttpComponent;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.ssl.TrustStrategy;
import org.cache2k.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

public class ConsulKVStore {

    private static final Logger log = LoggerFactory.getLogger(ConsulKVStore.class);
    private final RestTemplate restTemplate;
    private final Cache<String, List<String>> corsHeadersCache;
    private final Cache<String, ConsulKeyStoreEntry> consulTrustStoreCache;
    private final RouteUtils routeUtils;
    private final String consulKvHost;
    private final String consulKvToken;
    private final String capiTrustStorePassword;
    private ConsulNodeDiscovery consulNodeDiscovery;
    private CapiSslContextHolder capiSslContextHolder;
    private CamelContext camelContext;

    public ConsulKVStore(RestTemplate restTemplate, Cache<String, List<String>> corsHeadersCache, Cache<String, ConsulKeyStoreEntry> consulTrustStoreCache, RouteUtils routeUtils, String consulKvHost, String consulKvToken, String capiTrustStorePassword, ConsulNodeDiscovery consulNodeDiscovery, CapiSslContextHolder capiSslContextHolder, CamelContext camelContext) {
        this.restTemplate = restTemplate;
        this.corsHeadersCache = corsHeadersCache;
        this.consulTrustStoreCache = consulTrustStoreCache;
        this.routeUtils = routeUtils;
        this.consulKvHost = consulKvHost;
        this.consulKvToken = consulKvToken;
        this.capiTrustStorePassword = capiTrustStorePassword;
        this.consulNodeDiscovery = consulNodeDiscovery;
        this.capiSslContextHolder = capiSslContextHolder;
        this.camelContext = camelContext;
    }

    public void process() {
        log.debug("Looking for key values...");
        capiCorsHeadersKVCall();
        syncTrustStore();
    }

    private void capiCorsHeadersKVCall() {
        List<String> cachedValueAsList = corsHeadersCache.get("capi-cors-headers");
        HttpEntity<Void> request;
        try {
            if(consulKvToken != null) {
                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(consulKvToken);
                request = new HttpEntity<>(headers);
            } else {
                request = new HttpEntity<>(null);
            }
            ResponseEntity<ConsulKeyStoreEntry[]> consulKeyValueStoreResponse = restTemplate.exchange(consulKvHost + Constants.CONSUL_KV_STORE_API + Constants.CAPI_CORS_HEADERS_CACHE_KEY, HttpMethod.GET, request, ConsulKeyStoreEntry[].class);
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

    private void syncTrustStore() {
        ConsulKeyStoreEntry cachedTrustStore = consulTrustStoreCache.get(Constants.CONSUL_CAPI_TRUST_STORE_GROUP_KEY);
        ConsulKeyStoreEntry remoteTrustStore = getRemoteTrustStore();
        try {
            //Found remote
            if(remoteTrustStore != null ) {
                if(cachedTrustStore != null) {
                    if(remoteTrustStore.getModifyIndex() != cachedTrustStore.getModifyIndex()) {
                        log.debug("The remote object is different from the local, lets update the local");
                        processTrustStore(remoteTrustStore);
                        consulTrustStoreCache.put(Constants.CONSUL_CAPI_TRUST_STORE_GROUP_KEY, remoteTrustStore);
                    } else {
                        log.debug("The remote object is equal to the local, nothing to do for now.");
                        consulTrustStoreCache.put(Constants.CONSUL_CAPI_TRUST_STORE_GROUP_KEY, remoteTrustStore);
                    }
                } else {
                    log.debug("Found remote trust store but not local, CAPI will cache the remote for the first time.");
                    processTrustStore(remoteTrustStore);
                    consulTrustStoreCache.put(Constants.CONSUL_CAPI_TRUST_STORE_GROUP_KEY, remoteTrustStore);
                }
            } else {
                log.debug("No remote keystore found, so nothing to do for now.");
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private void updateConsulHeaderKey(String key, String value) {
        HttpEntity<String> request;
        if(consulKvToken != null) {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(consulKvToken);
            request = new HttpEntity<>(value, headers);
        } else {
            request = new HttpEntity<>(value);
        }
        restTemplate.put(consulKvHost + Constants.CONSUL_KV_STORE_API + key, request);
    }

    private List<String> consulKeyValueToList(String encodedValue) {
        String decodedValue = new String(Base64.getDecoder().decode(encodedValue));
        return Arrays.asList(decodedValue.split(",", -1));
    }

    private ConsulKeyStoreEntry getRemoteTrustStore() {
        if(consulKvHost != null) {
            HttpEntity<Void> request;
            try {
                if(consulKvToken != null) {
                    HttpHeaders headers = new HttpHeaders();
                    headers.setBearerAuth(consulKvToken);
                    request = new HttpEntity<>(headers);
                } else {
                    request = new HttpEntity<>(null);
                }
                ResponseEntity<ConsulKeyStoreEntry[]> consulKeyValueStoreResponse = restTemplate.exchange(
                        consulKvHost + Constants.CONSUL_KV_STORE_API + Constants.CONSUL_CAPI_TRUST_STORE_GROUP_KEY, HttpMethod.GET, request, ConsulKeyStoreEntry[].class);
                if (consulKeyValueStoreResponse.getStatusCode().is2xxSuccessful()) {
                    return Objects.requireNonNull(consulKeyValueStoreResponse.getBody())[0];
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
        return null;
    }

    public InputStream consulKeyValueToInputStream(String encodedValue) throws JsonProcessingException {
        String decodedValue = new String(Base64.getDecoder().decode(encodedValue));
        return new ByteArrayInputStream(Base64.getDecoder().decode(decodedValue.getBytes()));
    }

    private void processTrustStore(ConsulKeyStoreEntry trustStoreConsulKeyStoreEntry) throws IOException {
        try (InputStream trustStoreInputStream = consulKeyValueToInputStream(trustStoreConsulKeyStoreEntry.getValue())) {
            routeUtils.reloadTrustStoreManager(trustStoreInputStream, capiTrustStorePassword);
        }

        try {
            HttpComponent httpComponent = (HttpComponent) camelContext.getComponent("https");
            CapiTrustManager capiTrustManager = (CapiTrustManager) httpComponent.getSslContextParameters().getTrustManagers().getTrustManager();
            TrustStrategy trustStrategy = (X509Certificate[] chain, String authType) -> false;
            SSLContext sslContext = SSLContextBuilder
                        .create()
                        .loadTrustMaterial(capiTrustManager.getKeyStore(), trustStrategy)
                        .build();
            capiSslContextHolder.setSslContext(sslContext);
            consulNodeDiscovery.reloadHttpClient();
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
            throw new RuntimeException(e);
        }
    }
}