package io.surisoft.capi.lb.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
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
import io.surisoft.capi.lb.utils.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.HashMap;

@Configuration
@Slf4j
public class ManagerSecurityConfig extends WebSecurityConfigurerAdapter {

    @Value("${capi.manager.security.enabled}")
    private boolean capiManagerSecurityEnabled;

    @Value("${capi.manager.security.issuer}")
    private String capiManagerSecurityIssuer;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        log.debug("Configuring security");
        if(capiManagerSecurityEnabled) {
            http
                    .csrf()
                    .disable()
                    .sessionManagement()
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                    .and()
                    .authorizeRequests()
                    .antMatchers(Constants.CAPI_WHITELISTED_PATHS)
                    .permitAll()
                    .and()
                    .authorizeRequests(authz -> authz
                            .anyRequest().authenticated())
                    .oauth2ResourceServer(oauth2 -> oauth2.jwt().decoder(new CapiJWTDecoder()));
        } else {
            http
                    .csrf()
                    .disable()
                    .sessionManagement()
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                    .and()
                    .authorizeRequests().anyRequest().permitAll();
        }

    }

    class CapiJWTDecoder implements JwtDecoder {

        @Override
        public Jwt decode(String token) throws JwtException {
            try {
                SignedJWT decodedToken = SignedJWT.parse(token);
                TypeReference<HashMap<String, Object>> typeReference = new TypeReference<HashMap<String, Object>>() {};
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