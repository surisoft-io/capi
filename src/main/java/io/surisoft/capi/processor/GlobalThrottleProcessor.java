package io.surisoft.capi.processor;

import io.surisoft.capi.exception.AuthorizationException;
import io.surisoft.capi.oidc.Oauth2Constants;
import io.surisoft.capi.schema.Service;
import io.surisoft.capi.schema.ThrottleServiceObject;
import io.surisoft.capi.throttle.ThrottleCacheManager;
import io.surisoft.capi.utils.Constants;
import io.surisoft.capi.utils.HttpUtils;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.cache2k.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "capi.throttling", name = "enabled", havingValue = "true")
public class GlobalThrottleProcessor implements Processor {

    private static final Logger log = LoggerFactory.getLogger(GlobalThrottleProcessor.class);
    private final Cache<String, Service> serviceCache;
    private final ThrottleCacheManager throttleCacheManager;
    private final HttpUtils httpUtils;

    public GlobalThrottleProcessor(Cache<String, Service> serviceCache, ThrottleCacheManager throttleCacheManager, HttpUtils httpUtils) {
        this.serviceCache = serviceCache;
        this.throttleCacheManager = throttleCacheManager;
        this.httpUtils = httpUtils;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        try {
            String contextPath = (String) exchange.getIn().getHeader(Oauth2Constants.CAMEL_SERVLET_CONTEXT_PATH);
            Service service = serviceCache.get(httpUtils.contextToRole(contextPath));
            long currentCall = 0;
            if(service != null) {
                ThrottleServiceObject throttleServiceObject = throttleCacheManager.getGlobal(service.getId());
                if(throttleServiceObject != null) {
                    if(throttleServiceObject.getCurrentCalls() > throttleServiceObject.getTotalCallsAllowed()) {
                        //exchange.getIn().setHeader(Constants.CAPI_SHOULD_THROTTLE, true);
                        //exchange.getIn().setHeader(Constants.CAPI_THROTTLE_DURATION_MILLI, throttleServiceObject.remainingTime());
                        exchange.getIn().setHeader(Constants.REASON_MESSAGE_HEADER, "Too Many requests");
                        exchange.getIn().setHeader(Constants.REASON_CODE_HEADER, HttpStatus.TOO_MANY_REQUESTS.value());
                        exchange.setException(new AuthorizationException("Too Many requests"));
                    } else {
                        currentCall = throttleServiceObject.getCurrentCalls() + 1;
                        throttleServiceObject.setCurrentCalls(currentCall);
                        throttleCacheManager.updateGlobal(service.getId(), throttleServiceObject.remainingTime(), throttleServiceObject);
                        exchange.getIn().setHeader(Constants.CAPI_SHOULD_THROTTLE, false);
                    }
                } else {
                    currentCall = 1;
                    throttleServiceObject = new ThrottleServiceObject(service.getId(),
                            exchange.getIn().getHeader(Constants.CAPI_META_THROTTLE_DURATION, Long.TYPE),
                            exchange.getIn().getHeader(Constants.CAPI_META_THROTTLE_TOTAL_CALLS_ALLOWED, Long.TYPE));
                    throttleServiceObject.setCurrentCalls(currentCall);
                    throttleCacheManager.insertGlobal(service.getId(), throttleServiceObject.getExpirationDuration(), throttleServiceObject);
                    exchange.getIn().setHeader(Constants.CAPI_SHOULD_THROTTLE, false);
                }
            }
        } catch(Exception e) {
            log.warn(e.getMessage(), e);
        }
    }
}