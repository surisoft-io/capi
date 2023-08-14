package io.surisoft.capi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public final class CapiTrustManager implements X509TrustManager {

    private static final Logger log = LoggerFactory.getLogger(CapiTrustManager.class);
    private final String capiTrustStorePassword;
    private final String capiTrustStorePath;
    private X509TrustManager trustManager;

    public CapiTrustManager(String capiTrustStorePath, String capiTrustStorePassword) throws Exception {
        log.info("Starting CAPI Trust Store Manager");
        this.capiTrustStorePassword = capiTrustStorePassword;
        this.capiTrustStorePath = capiTrustStorePath;
        reloadTrustManager();
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        trustManager.checkClientTrusted(chain, authType);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        trustManager.checkServerTrusted(chain, authType);
    }


    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return trustManager.getAcceptedIssuers();
    }

    public void reloadTrustManager() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        try (InputStream in = new FileInputStream(capiTrustStorePath)) {
            keyStore.load(in, capiTrustStorePassword.toCharArray());
        }

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);

        TrustManager[] trustManagerList = trustManagerFactory.getTrustManagers();
        for(TrustManager tempTrustManager : trustManagerList) {
            if (tempTrustManager instanceof X509TrustManager) {
                trustManager = (X509TrustManager) tempTrustManager;
            }
        }
    }
}