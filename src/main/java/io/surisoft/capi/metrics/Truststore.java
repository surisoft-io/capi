package io.surisoft.capi.metrics;

import io.surisoft.capi.schema.AliasInfo;
import io.surisoft.capi.service.CapiTrustManager;
import io.surisoft.capi.utils.Constants;
import org.apache.camel.CamelContext;
import org.apache.camel.component.http.HttpComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.endpoint.annotation.*;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

@Component
@Endpoint(id = "truststore")
public class Truststore {
    private static final Logger log = LoggerFactory.getLogger(Truststore.class);
    private final String capiTrustStorePath;
    private final String capiTrustStorePassword;
    private final boolean capiTrustStoreEnabled;
    private final ResourceLoader resourceLoader;
    private final RestTemplate restTemplate;
    private final String consulKvHost;
    private final String consulKvToken;
    private final CamelContext camelContext;

    public Truststore(ResourceLoader resourceLoader,
                      RestTemplate restTemplate,
                      @Value("${capi.trust.store.enabled}") boolean capiTrustStoreEnabled,
                      @Value("${capi.trust.store.path}") String capiTrustStorePath,
                      @Value("${capi.trust.store.password}") String capiTrustStorePassword,
                      @Value("${capi.consul.kv.host}") String consulKvHost,
                      @Value("${capi.consul.kv.token}") String consulKvToken,
                      CamelContext camelContext) {
        this.resourceLoader = resourceLoader;
        this.restTemplate = restTemplate;
        this.capiTrustStoreEnabled = capiTrustStoreEnabled;
        this.capiTrustStorePath = capiTrustStorePath;
        this.capiTrustStorePassword = capiTrustStorePassword;
        this.consulKvHost = consulKvHost;
        this.consulKvToken = consulKvToken;
        this.camelContext = camelContext;
    }

    @ReadOperation
    public List<AliasInfo> getTruststore() {
        List<AliasInfo> aliasList = new ArrayList<>();

        if(!capiTrustStoreEnabled) {
            AliasInfo aliasInfo = new AliasInfo();
            aliasInfo.setAdditionalInfo(Constants.NO_CUSTOM_TRUST_STORE_PROVIDED);
            aliasList.add(aliasInfo);
            return aliasList;
        }

        try {
            HttpComponent httpComponent = (HttpComponent) camelContext.getComponent("https");
            CapiTrustManager capiTrustManager = (CapiTrustManager) httpComponent.getSslContextParameters().getTrustManagers().getTrustManager();

            KeyStore keystore = capiTrustManager.getKeyStore();
            Enumeration<String> aliases = keystore.aliases();
            while(aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                AliasInfo aliasInfo = new AliasInfo();
                aliasInfo.setAlias(alias);
                X509Certificate certificate = (X509Certificate) keystore.getCertificate(alias);
                aliasInfo.setIssuerDN(certificate.getIssuerX500Principal().getName());
                aliasInfo.setSubjectDN(certificate.getSubjectX500Principal().getName());
                aliasInfo.setNotBefore(certificate.getNotBefore());
                aliasInfo.setNotAfter(certificate.getNotAfter());
                aliasList.add(aliasInfo);
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
        return aliasList;
    }

    @DeleteOperation
    public ResponseEntity<AliasInfo> deleteAlias(@Selector String alias) {
        AliasInfo aliasInfo = new AliasInfo();

        if(!capiTrustStoreEnabled) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        try(InputStream is = getInputStream()) {
            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            keystore.load(is, capiTrustStorePassword.toCharArray());

            keystore.deleteEntry(alias);

            try(OutputStream storeOutputStream = getOutputStream()) {
                keystore.store(storeOutputStream, capiTrustStorePassword.toCharArray());
            }
        } catch (Exception e) {
            log.debug(e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        aliasInfo.setAlias(alias);
        return new ResponseEntity<>(aliasInfo, HttpStatus.OK);
    }

    @WriteOperation
    public ResponseEntity<AliasInfo> addCertificate(String fileBlob) {
        if(capiTrustStoreEnabled && consulKvHost != null) {
            HttpEntity<String> consulRequest;
            if(consulKvToken != null) {
                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(consulKvToken);
                consulRequest = new HttpEntity<>(fileBlob, headers);
            } else {
                consulRequest = new HttpEntity<>(fileBlob);
            }
            ResponseEntity<Void> response = restTemplate.exchange(consulKvHost + Constants.CONSUL_KV_STORE_API + Constants.CONSUL_CAPI_TRUST_STORE_GROUP_KEY, HttpMethod.PUT, consulRequest, Void.class);
            if(response.getStatusCode().is2xxSuccessful()) {
                return new ResponseEntity<>(HttpStatus.CREATED);
            } else {
                log.error("Error from Persisting Keystore in Consul: {}", response.getStatusCode());
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        } else {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    private InputStream getInputStream() throws IOException {
        if(capiTrustStorePath.startsWith("classpath")) {
            return resourceLoader.getResource(capiTrustStorePath).getInputStream();
        } else {
            return new FileInputStream(capiTrustStorePath);
        }
    }

    private OutputStream getOutputStream() throws IOException {
        if(capiTrustStorePath.startsWith("classpath")) {
            return new FileOutputStream(resourceLoader.getResource(capiTrustStorePath).getFile());
        } else {
            return new FileOutputStream(capiTrustStorePath);
        }
    }
}