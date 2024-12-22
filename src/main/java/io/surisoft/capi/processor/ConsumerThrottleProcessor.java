package io.surisoft.capi.processor;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.surisoft.capi.oidc.Oauth2Constants;
import io.surisoft.capi.schema.OpaResult;
import io.surisoft.capi.schema.Service;
import io.surisoft.capi.schema.ThrottleServiceObject;
import io.surisoft.capi.service.OpaService;
import io.surisoft.capi.throttle.ThrottleCacheManager;
import io.surisoft.capi.utils.Constants;
import io.surisoft.capi.utils.HttpUtils;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.cache2k.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@ConditionalOnProperty(prefix = "capi.throttling", name = "enabled", havingValue = "true")
public class ConsumerThrottleProcessor implements Processor {

    private static final Logger log = LoggerFactory.getLogger(ConsumerThrottleProcessor.class);
    private final Cache<String, Service> serviceCache;
    private final ThrottleCacheManager throttleCacheManager;
    private final HttpUtils httpUtils;
    private final Optional<OpaService> opaService;

    public ConsumerThrottleProcessor(Cache<String, Service> serviceCache, ThrottleCacheManager throttleCacheManager, HttpUtils httpUtils, Optional<OpaService> opaService) {
        this.serviceCache = serviceCache;
        this.throttleCacheManager = throttleCacheManager;
        this.httpUtils = httpUtils;
        this.opaService = opaService;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        try {
            if(opaService.isPresent()) {
                String contextPath = (String) exchange.getIn().getHeader(Oauth2Constants.CAMEL_SERVLET_CONTEXT_PATH);
                String accessToken = httpUtils.processAuthorizationAccessToken(exchange);
                Service service = serviceCache.get(httpUtils.contextToRole(contextPath));
                long currentCall = 0;
                if(service != null && accessToken != null) {
                    SignedJWT signedJWT = SignedJWT.parse(accessToken);
                    JWTClaimsSet jwtClaimsSet = signedJWT.getJWTClaimsSet();
                    String consumerKey = jwtClaimsSet.getStringClaim("azp");
                    if(consumerKey != null) {
                        ThrottleServiceObject throttleServiceObject = throttleCacheManager.getConsumer(service.getId());
                        if(throttleServiceObject != null) {
                            if(throttleServiceObject.getCurrentCalls() > throttleServiceObject.getTotalCallsAllowed()) {
                                exchange.getIn().setHeader(Constants.CAPI_SHOULD_THROTTLE, true);
                                exchange.getIn().setHeader(Constants.CAPI_THROTTLE_DURATION_MILLI, throttleServiceObject.remainingTime());
                            } else {
                                currentCall = throttleServiceObject.getCurrentCalls() + 1;
                                throttleServiceObject.setCurrentCalls(currentCall);
                                throttleCacheManager.updateConsumer(service.getId(), throttleServiceObject.remainingTime(), throttleServiceObject);
                                exchange.getIn().setHeader(Constants.CAPI_SHOULD_THROTTLE, false);
                            }
                        } else {
                            currentCall = 1;
                            OpaResult opaResult = opaService.get().callOpa(service.getServiceMeta().getOpaRego(), consumerKey, false);
                            if(opaResult != null &&
                                    opaResult.isAllowed() &&
                                    opaResult.getDuration() > -1 &&
                                    opaResult.getTotalCallsAllowed() > -1) {

                                throttleServiceObject = new ThrottleServiceObject(service.getId(),
                                        opaResult.getDuration(),
                                        opaResult.getTotalCallsAllowed());
                                throttleServiceObject.setCurrentCalls(currentCall);
                                throttleCacheManager.insertConsumer(service.getId(), throttleServiceObject.getExpirationDuration(), throttleServiceObject);
                                exchange.getIn().setHeader(Constants.CAPI_SHOULD_THROTTLE, false);
                            } else {
                                httpUtils.sendException(exchange, "Not allowed to call opa: " + opaResult);
                            }
                        }
                    } else {
                        httpUtils.sendException(exchange, "No consumer key found!");
                    }
                } else {
                   httpUtils.sendException(exchange, "No service found!");
                }
            } else {
                httpUtils.sendException(exchange, "OPA not available!");
            }
        } catch(Exception e) {
            log.warn(e.getMessage(), e);
            httpUtils.sendException(exchange, e.getMessage());
        }
    }
}