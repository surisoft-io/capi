package io.surisoft.capi.processor;

import io.surisoft.capi.cache.StickySessionCacheManager;
import io.surisoft.capi.schema.StickySession;
import io.surisoft.capi.utils.Constants;
import org.apache.camel.*;
import org.apache.camel.processor.loadbalancer.ExceptionFailureStatistics;
import org.apache.camel.processor.loadbalancer.LoadBalancerSupport;
import org.apache.camel.support.ExchangeHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

public class StickyLoadBalancer extends LoadBalancerSupport implements Traceable, CamelContextAware {

    private static final Logger log = LoggerFactory.getLogger(StickyLoadBalancer.class);
    private CamelContext camelContext;
    private final StickySessionCacheManager stickySessionCacheManager;
    private final boolean isCookie;
    private final String paramName;
    private boolean roundRobin;
    private final AtomicInteger counter = new AtomicInteger(-1);
    private final AtomicInteger lastGoodIndex = new AtomicInteger(-1);
    private final ExceptionFailureStatistics statistics = new ExceptionFailureStatistics();

    public StickyLoadBalancer(StickySessionCacheManager stickySessionCacheManager, String paramName, boolean isCookie) {
        this.stickySessionCacheManager = stickySessionCacheManager;
        this.isCookie = isCookie;
        this.paramName = paramName;
        this.roundRobin = true;
    }

    @Override
    public boolean process(Exchange exchange, AsyncCallback callback) {
        AsyncProcessor[] processors = doGetProcessors();
        exchange
                .getContext()
                .getCamelContextExtension()
                .getReactiveExecutor()
                .schedule(new StickyLoadBalancer.State(exchange, callback, processors)::run);
        return false;
    }

    public boolean isRoundRobin() {
        return roundRobin;
    }

    public void setRoundRobin(boolean roundRobin) {
        this.roundRobin = roundRobin;
    }

    private String getCookieValue(Exchange exchange) {
        String paramValue = null;
        if(exchange.getIn().getHeader(Constants.COOKIE_HEADER) != null) {
            String[] cookieArray = exchange.getIn().getHeader(Constants.COOKIE_HEADER, String.class).split(";");
            for(String cookie : cookieArray) {
                if(cookie.startsWith(paramName)) {
                    String[] keyValue = cookie.split("=");
                    paramValue = keyValue[1].trim();
                }
            }
        }
        return paramValue;
    }

    protected boolean shouldFailOver(Exchange exchange, boolean firstTime, StickySession stickySession) {
        if (exchange == null) {
            return false;
        }

        boolean answer = false;
        if(exchange.getMessage() != null && exchange.getMessage().getHeader(Constants.SET_COOKIE_HEADER) != null) {
            String[] serviceCookieArray = exchange.getMessage().getHeader(Constants.SET_COOKIE_HEADER, String.class).split(";");
            for(String cookie : serviceCookieArray) {
                if(cookie.contains(stickySession.getParamName())) {
                    stickySession.setParamValue(cookie.split("=")[1]);
                }
            }
        }
        String exchangeCode = exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE) + "";
        if(exchangeCode.startsWith("5")) {
            if(firstTime) {
               answer = true;
            } else {
                log.info("An existing Session failed to contact the node, dropping session: {}", stickySession.getParamValue());
                deleteFailedSession(stickySession);
            }
        }
        return answer;
    }

    @Override
    public boolean isRunAllowed() {
        // determine if we can still run, or the camel context is forcing a shutdown
        boolean forceShutdown = camelContext.getShutdownStrategy().isForceShutdown();
        if (forceShutdown) {
            log.trace("Run not allowed as ShutdownStrategy is forcing shutting down");
        }
        return !forceShutdown && super.isRunAllowed();
    }

    private void persistProcessedIndex(StickySession stickySession) {
        if(stickySession.getParamValue() != null) {
            stickySessionCacheManager.createStickySession(stickySession, true);
        }
    }

    private void deleteFailedSession(StickySession stickySession) {
        log.debug("Deleting object with value: {}", stickySession.getParamValue());
        stickySessionCacheManager.deleteStickySession(stickySession);
        stickySession.setParamValue(null);
    }

    protected class State {

        final Exchange exchange;
        final AsyncCallback callback;
        final AsyncProcessor[] processors;
        int index;
        int attempts;
        boolean firstTime = false;
        StickySession stickySession = null;

        // use a copy of the original exchange before failover to avoid populating side effects
        // directly into the original exchange
        Exchange copy;

        public State(Exchange exchange, AsyncCallback callback, AsyncProcessor[] processors) {
            this.exchange = exchange;
            this.callback = callback;
            this.processors = processors;

            String paramValue = null;
            if(isCookie) {
                paramValue = getCookieValue(exchange);
            } else {
                paramValue = exchange.getIn().getHeader(paramName, String.class);
            }
            log.info("Starting to process route for paramName: {} with value: {}", paramName, paramValue);
            stickySession = stickySessionCacheManager.getStickySessionById(paramName, paramValue);
            if(stickySession == null) {
                log.debug("New value, processing for the first time...");
                index = counter.updateAndGet(x -> ++x < processors.length ? x : 0);
                firstTime = true;
                stickySession = new StickySession();
                stickySession.setParamValue(paramValue);
                stickySession.setParamName(paramName);
                stickySession.setNodeIndex(index);
            } else {
                log.debug("Already existing config with index:" + stickySession.getNodeIndex());
                index = stickySession.getNodeIndex();
            }
            log.trace("SessionCheckerFailover starting with endpoint index {}", index);
        }

        public void run() {
            if (copy != null && !shouldFailOver(copy, firstTime, stickySession)) {
                // remember last good index
                lastGoodIndex.set(index);
                // and copy the current result to original so it will contain this result of this eip
                ExchangeHelper.copyResults(exchange, copy);
                if (log.isDebugEnabled()) {
                    log.debug("Failover complete for exchangeId: {} >>> {}", exchange.getExchangeId(), exchange);
                }
                persistProcessedIndex(stickySession);
                callback.done(false);
                return;
            }

            // can we still run
            if (!isRunAllowed()) {
                log.trace("Run not allowed, will reject executing exchange: {}", exchange);
                if (exchange.getException() == null) {
                    exchange.setException(new RejectedExecutionException());
                }
                // we cannot process so invoke callback
                callback.done(false);
                return;
            }

            if (copy != null) {
                attempts++;
                int maximumFailoverAttempts = -1;
                if (attempts > maximumFailoverAttempts) {
                    log.debug("Breaking out of failover after {} failover attempts", attempts);
                    ExchangeHelper.copyResults(exchange, copy);
                    callback.done(false);
                    return;
                }
                index++;
                counter.incrementAndGet();
            }

            if (index >= processors.length) {
                // out of bounds
                if (isRoundRobin()) {
                    log.trace("Failover is round robin enabled and therefore starting from the first endpoint");
                    index = 0;
                    counter.set(0);
                } else {
                    // no more processors to try
                    log.trace("Breaking out of failover as we reached the end of endpoints to use for failover");
                    ExchangeHelper.copyResults(exchange, copy);
                    callback.done(false);
                    return;
                }
            }

            // try again but copy original exchange before we failover
            copy = prepareExchangeForFailover(exchange);
            AsyncProcessor processor = processors[index];

            // process the exchange
            log.debug("Processing failover at attempt {} for {}", attempts, copy);
            processor.process(copy, doneSync -> exchange.getContext().getCamelContextExtension().getReactiveExecutor()
                    .schedule(this::run));
        }
    }

    @Override
    public CamelContext getCamelContext() {
        return camelContext;
    }

    @Override
    public void setCamelContext(CamelContext camelContext) {
        this.camelContext = camelContext;
    }

    protected Exchange prepareExchangeForFailover(Exchange exchange) {
        // use a copy of the exchange to avoid side effects on the original exchange
        return ExchangeHelper.createCopy(exchange, true);
    }

    @Override
    public String getTraceLabel() {
        return "failover";
    }

    public void reset() {
        lastGoodIndex.set(-1);
        counter.set(-1);
        statistics.reset();
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        reset();
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
    }
}