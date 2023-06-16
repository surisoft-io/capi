package io.surisoft.capi.lb.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import io.surisoft.capi.lb.oidc.OIDCConstants;
import io.surisoft.capi.lb.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.web.SecurityFilterChain;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.HashMap;

@Configuration
@EnableWebSecurity
public class ManagerSecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(ManagerSecurityConfig.class);

    @Value("${capi.manager.security.enabled}")
    private boolean capiManagerSecurityEnabled;

    @Value("${capi.manager.security.issuer}")
    private String capiManagerSecurityIssuer;

    @Value("${oidc.provider.host}")
    private String oidcProviderHost;

    @Value("${oidc.provider.realm}")
    private String oidcProviderRealm;

    @Bean
    protected SecurityFilterChain configure(HttpSecurity http) throws Exception {
        log.debug("Configuring security");
        if(capiManagerSecurityEnabled) {
            return http
                    .csrf()
                    .disable()
                    .sessionManagement()
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                    .and()
                    .authorizeHttpRequests().requestMatchers(Constants.CAPI_WHITELISTED_PATHS)
                    //.antMatchers(Constants.CAPI_WHITELISTED_PATHS)
                    .permitAll()
                    .and()
                    .authorizeHttpRequests(authz -> authz
                            .anyRequest().authenticated())
                    .oauth2ResourceServer(oauth2 -> oauth2.jwt().decoder(new CapiJWTDecoder()))
                    .cors()
                    .and()
                    .build();
        } else {
            return http
                    .csrf()
                    .disable()
                    .sessionManagement()
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                    .and()
                    .authorizeHttpRequests().anyRequest().permitAll().and().build();
        }
    }

    @Bean
    @ConditionalOnProperty(prefix = "oidc.provider", name = "enabled", havingValue = "true")
    public DefaultJWTProcessor<SecurityContext> getJwtProcessor() throws IOException, ParseException {
        log.trace("Starting CAPI JWT Processor");
        DefaultJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
        JWKSet jwkSet = JWKSet.load(new URL(oidcProviderHost + oidcProviderRealm + OIDCConstants.CERTS_URI));
        ImmutableJWKSet<SecurityContext> keySource = new ImmutableJWKSet<>(jwkSet);
        JWSAlgorithm expectedJWSAlg = JWSAlgorithm.RS256;
        JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(expectedJWSAlg, keySource);
        jwtProcessor.setJWSKeySelector(keySelector);
        return jwtProcessor;
    }

    class CapiJWTDecoder implements JwtDecoder {

        @Override
        public Jwt decode(String token) throws JwtException {
            try {
                SignedJWT decodedToken = SignedJWT.parse(token);
                TypeReference<HashMap<String, Object>> typeReference = new TypeReference<>() {};
                HashMap<String, Object> headerMap = new ObjectMapper().readValue(decodedToken.getHeader().toString(), typeReference);

                ConfigurableJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
                JWKSource<SecurityContext> keySource = new RemoteJWKSet<>(new URL(capiManagerSecurityIssuer));
                JWSAlgorithm expectedJWSAlg = JWSAlgorithm.RS256;
                JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(expectedJWSAlg, keySource);
                jwtProcessor.setJWSKeySelector(keySelector);
                JWTClaimsSet claimsSet = jwtProcessor.process(token, null);

                return new Jwt(token,
                        claimsSet.getIssueTime().toInstant(),
                        claimsSet.getExpirationTime().toInstant(),
                        headerMap,
                        claimsSet.getClaims());

            } catch(ParseException | BadJOSEException | JOSEException | MalformedURLException | JsonProcessingException e) {
                log.info(e.getMessage());
                throw new JwtException("Token Expired");
            }
        }
    }
}