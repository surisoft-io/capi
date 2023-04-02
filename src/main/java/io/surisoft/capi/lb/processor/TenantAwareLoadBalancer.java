package io.surisoft.capi.lb.processor;

import io.surisoft.capi.lb.utils.Constants;
import org.apache.camel.*;
import org.apache.camel.processor.loadbalancer.ExceptionFailureStatistics;
import org.apache.camel.processor.loadbalancer.LoadBalancerSupport;
import org.apache.camel.support.ExchangeHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.RejectedExecutionException;

public class TenantAwareLoadBalancer extends LoadBalancerSupport implements Traceable, CamelContextAware {
    private static final Logger log = LoggerFactory.getLogger(TenantAwareLoadBalancer.class);
    private CamelContext camelContext;
    private final ExceptionFailureStatistics statistics = new ExceptionFailureStatistics();

    @Override
    public boolean process(Exchange exchange, AsyncCallback callback) {
        AsyncProcessor[] processors = doGetProcessors();
        exchange.getContext().getCamelContextExtension().getReactiveExecutor()
                .schedule(new TenantAwareLoadBalancer.State(exchange, callback, processors)::run);
        return false;
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

    protected class State {
        final Exchange exchange;
        final AsyncCallback callback;
        final AsyncProcessor[] processors;
        int index = -1;
        Exchange copy;
        String tenant;
        public State(Exchange exchange, AsyncCallback callback, AsyncProcessor[] processors) {
            this.exchange = exchange;
            this.callback = callback;
            this.processors = processors;

            if(exchange.getIn().getHeader("tenant") != null) {
                //For tenant, it will always get the value from a header
                tenant = exchange.getIn().getHeader("tenant", String.class);
                log.info(tenant);
                for(int i = 0; i < processors.length; i++) {
                    if(processors[i].toString().contains("tenantId=" + tenant)) {
                        index = i;
                    }
                }

            }
        }

        public void run() {

            if(index == -1) {
                exchange.getIn().setHeader(Constants.CAPI_INTERNAL_ERROR, "No tenant available to process " + (tenant != null ? tenant : ""));
                exchange.setException(new CamelException("No tenant available"));
                callback.done(false);
                return;
            } else {
                if (copy != null) {

                    ExchangeHelper.copyResults(exchange, copy);
                    if (log.isDebugEnabled()) {
                        log.debug("Tenant aware complete for exchangeId: {} >>> {}", exchange.getExchangeId(), exchange);
                    }

                    if(exchange.getProperty(Exchange.FAILURE_HANDLED) != null) {
                        exchange.getIn().setHeader(Constants.CAPI_INTERNAL_ERROR, "Tenant aware completed in error, tenant " + tenant + " not available, please check the node");
                        log.debug("Tenant aware completed in error, tenant {} not available, please check the node", tenant);
                    }
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

                // try again but copy original exchange before we failover
                copy = prepareExchange(exchange);
                AsyncProcessor processor = processors[index];

                // process the exchange
                log.debug("Processing Tenant {} for {}", tenant, copy);
                processor.process(copy, doneSync -> exchange.getContext().getCamelContextExtension().getReactiveExecutor()
                        .schedule(this::run));
            }
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

    protected Exchange prepareExchange(Exchange exchange) {
        return ExchangeHelper.createCopy(exchange, true);
    }

    @Override
    public String getTraceLabel() {
        return "Tenant Aware Processor";
    }

    public void reset() {
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