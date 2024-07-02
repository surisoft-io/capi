package io.surisoft.capi.metrics;

import io.surisoft.capi.schema.AliasInfo;
import io.surisoft.capi.utils.Constants;
import io.surisoft.capi.utils.RouteUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.endpoint.annotation.*;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.io.*;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
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
    private final RouteUtils routeUtils;

    public Truststore(ResourceLoader resourceLoader,
                      RouteUtils routeUtils,
                      @Value("${capi.trust.store.enabled}") boolean capiTrustStoreEnabled,
                      @Value("${capi.trust.store.path}") String capiTrustStorePath,
                      @Value("${capi.trust.store.password}") String capiTrustStorePassword) {
        this.resourceLoader = resourceLoader;
        this.routeUtils = routeUtils;
        this.capiTrustStoreEnabled = capiTrustStoreEnabled;
        this.capiTrustStorePath = capiTrustStorePath;
        this.capiTrustStorePassword = capiTrustStorePassword;
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

        try(InputStream is = getInputStream()) {
            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            keystore.load(is, capiTrustStorePassword.toCharArray());
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
            //routeUtils.reloadTrustStoreManager(apiId, true);
        } catch (Exception e) {
            log.debug(e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        aliasInfo.setAlias(alias);
        return new ResponseEntity<>(aliasInfo, HttpStatus.OK);
    }

    @WriteOperation
    public ResponseEntity<AliasInfo> addCertificate(String alias, String serviceId, String fileBlob) {
        AliasInfo aliasInfo = new AliasInfo();

        byte[] fileInfo = getBase64Value(fileBlob);

        if(!capiTrustStoreEnabled || fileInfo == null) {
            aliasInfo = new AliasInfo();
            aliasInfo.setAdditionalInfo(Constants.NO_CUSTOM_TRUST_STORE_PROVIDED);
            return new ResponseEntity<>(aliasInfo, HttpStatus.BAD_REQUEST);
        }

        try(InputStream is = getInputStream()) {
            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            keystore.load(is, capiTrustStorePassword.toCharArray());

            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            Certificate newTrusted = certificateFactory.generateCertificate(new ByteArrayInputStream(fileInfo));

            X509Certificate x509Object = (X509Certificate) newTrusted;
            aliasInfo.setSubjectDN(x509Object.getSubjectX500Principal().getName());
            aliasInfo.setIssuerDN(x509Object.getIssuerX500Principal().getName());
            aliasInfo.setAlias(alias);
            aliasInfo.setServiceId(serviceId);

            keystore.setCertificateEntry(alias, newTrusted);

            try(OutputStream storeOutputStream = getOutputStream()) {
                keystore.store(storeOutputStream, capiTrustStorePassword.toCharArray());
            }
            routeUtils.reloadTrustStoreManager(serviceId, false);
        } catch (Exception e) {
            log.debug(e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

        }
        return new ResponseEntity<>(aliasInfo, HttpStatus.OK);
    }


    private InputStream getInputStream() throws IOException {
        log.trace(capiTrustStorePath);
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

    private byte[] getBase64Value(String fileBlob) {
        if(fileBlob.startsWith("data:application/octet-stream;base64,")) {
            return Base64.getDecoder().decode(fileBlob.replaceAll("data:application/octet-stream;base64,", ""));
        }
        return null;
    }
}