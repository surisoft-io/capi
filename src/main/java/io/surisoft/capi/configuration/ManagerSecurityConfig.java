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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import java.io.IOException;
import java.net.URL;
import java.text.ParseException;

@Configuration
@EnableWebSecurity
public class ManagerSecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(ManagerSecurityConfig.class);

    @Value("${oauth2.provider.keys}")
    private String  oauth2ProviderKeys;

    public ManagerSecurityConfig() {
    }


    @Bean
    protected SecurityFilterChain configure(HttpSecurity http) throws Exception {
        log.debug("Configuring security");
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement((sessionManagement) ->
                        sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(authorization ->
                        authorization.anyRequest().permitAll()
                )
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "oauth2.provider", name = "enabled", havingValue = "true")
    public DefaultJWTProcessor<SecurityContext> getJwtProcessor() throws IOException, ParseException {
        log.trace("Starting CAPI JWT Processor");
        DefaultJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
        JWKSet jwkSet = JWKSet.load(new URL(oauth2ProviderKeys));
        ImmutableJWKSet<SecurityContext> keySource = new ImmutableJWKSet<>(jwkSet);
        JWSAlgorithm expectedJWSAlg = JWSAlgorithm.RS256;
        JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(expectedJWSAlg, keySource);
        jwtProcessor.setJWSKeySelector(keySelector);
        return jwtProcessor;
    }
}