package io.surisoft.capi.configuration;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class ManagerSecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(ManagerSecurityConfig.class);

    public ManagerSecurityConfig() {
    }

    @Bean
    @ConfigurationProperties( prefix = "capi.oauth2.provider.keys" )
    public List<String> getOauth2ProviderKeys(){
        return new ArrayList<>();
    }


    @Bean
    @ConditionalOnProperty(prefix = "capi.oauth2.provider", name = "enabled", havingValue = "true")
    public List<DefaultJWTProcessor<SecurityContext>> getJwtProcessor() throws IOException, ParseException {
        log.trace("Starting CAPI JWT Processor");
        List<DefaultJWTProcessor<SecurityContext>> jwtProcessorList = new ArrayList<>();
        for(String jwk : getOauth2ProviderKeys()) {
            DefaultJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
            JWKSet jwkSet = JWKSet.load(new URL(jwk));
            ImmutableJWKSet<SecurityContext> keySource = new ImmutableJWKSet<>(jwkSet);
            JWSAlgorithm expectedJWSAlg = JWSAlgorithm.RS256;
            JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(expectedJWSAlg, keySource);
            jwtProcessor.setJWSKeySelector(keySelector);
            jwtProcessorList.add(jwtProcessor);
        }
        return jwtProcessorList;
    }
}