package io.surisoft.capi.lb.configuration;

import io.surisoft.capi.lb.service.CapiTrustManager;
import org.apache.camel.CamelContext;
import org.apache.camel.component.http.HttpComponent;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.camel.support.jsse.TrustManagersParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.File;
import java.io.IOException;

@Configuration
@ConditionalOnProperty(prefix = "capi.trust.store", name = "enabled", havingValue = "true")
public class CapiSslConfiguration {

    private static final Logger log = LoggerFactory.getLogger(CapiSslConfiguration.class);

    private String capiTrustStorePath;
    private String capiTrustStorePassword;

    private ResourceLoader resourceLoader;
    private CamelContext camelContext;

    public CapiSslConfiguration(CamelContext camelContext, ResourceLoader resourceLoader, @Value("${capi.trust.store.path}") String capiTrustStorePath, @Value("${capi.trust.store.password}") String capiTrustStorePassword) {
        this.camelContext = camelContext;
        this.resourceLoader = resourceLoader;
        this.capiTrustStorePath = capiTrustStorePath;
        this.capiTrustStorePassword = capiTrustStorePassword;
        createSslContext();
    }

    private void createSslContext() {
        try {
            log.info("Starting CAPI SSL Context");
            File filePath = getFile();

            HttpComponent httpComponent = (HttpComponent) camelContext.getComponent("https");

            CapiTrustManager capiTrustManager = new CapiTrustManager(filePath.getAbsolutePath(), capiTrustStorePassword);
            TrustManagersParameters trustManagersParameters = new TrustManagersParameters();
            trustManagersParameters.setTrustManager(capiTrustManager);

            SSLContextParameters sslContextParameters = new SSLContextParameters();
            sslContextParameters.setTrustManagers(trustManagersParameters);
            sslContextParameters.createSSLContext(camelContext);
            httpComponent.setSslContextParameters(sslContextParameters);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private File getFile() {
        File filePath = null;
        try {
            if(capiTrustStorePath.startsWith("classpath")) {
                Resource resource = resourceLoader.getResource(capiTrustStorePath);
                filePath = resource.getFile();
            } else {
                filePath = new File(capiTrustStorePath);
            }
        } catch(IOException e) {
            log.error(e.getMessage(), e);
        }
        return filePath;
    }
}
