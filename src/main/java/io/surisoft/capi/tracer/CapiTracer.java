package io.surisoft.capi.tracer;

import brave.Span;
import brave.Tracing;
import brave.context.slf4j.MDCScopeDecorator;
import brave.propagation.*;
import brave.sampler.Sampler;
import io.surisoft.capi.utils.Constants;
import io.surisoft.capi.utils.HttpUtils;
import org.apache.camel.*;
import org.apache.camel.api.management.ManagedAttribute;
import org.apache.camel.api.management.ManagedResource;
import org.apache.camel.spi.CamelEvent;
import org.apache.camel.spi.RoutePolicy;
import org.apache.camel.spi.RoutePolicyFactory;
import org.apache.camel.support.EventNotifierSupport;
import org.apache.camel.support.RoutePolicySupport;
import org.apache.camel.support.service.ServiceHelper;
import org.apache.camel.support.service.ServiceSupport;
import org.apache.camel.util.IOHelper;
import org.apache.camel.util.ObjectHelper;
import org.apache.camel.zipkin.CamelRequest;
import org.apache.camel.zipkin.ZipkinState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import zipkin2.reporter.Reporter;
import zipkin2.reporter.brave.ZipkinSpanHandler;

import java.io.Closeable;
import java.time.Instant;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalField;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.camel.util.URISupport.sanitizeUri;

@ManagedResource(description = "CapiTracer")
public class CapiTracer extends ServiceSupport implements RoutePolicyFactory, StaticService, CamelContextAware {
    private final HttpUtils httpUtils;
    private static final String REST_ROUTE = "rest://";
    private static final Logger LOG = LoggerFactory.getLogger(CapiTracer.class);
    private static final Propagation.Getter<CamelRequest, String> GETTER = CamelRequest::getHeader;
    private static final Propagation.Setter<CamelRequest, String> SETTER = CamelRequest::setHeader;
    static final TraceContext.Extractor<CamelRequest> EXTRACTOR = B3Propagation.B3_STRING.extractor(GETTER);
    private static final TraceContext.Injector<CamelRequest> INJECTOR = B3Propagation.B3_STRING.injector(SETTER);
    private final Map<String, Tracing> braves = new HashMap<>();
    private CamelContext camelContext;
    private String endpoint;
    private int port;
    private Reporter<zipkin2.Span> spanReporter;
    private final Map<String, String> serverServiceMappings = new HashMap<>();
    private boolean includeMessageBody;
    private boolean includeMessageBodyStreams;
    private final List<String> exclusions = new ArrayList<>();

    private final CapiEventNotifier eventNotifier = new CapiEventNotifier();

    public CapiTracer(HttpUtils httpUtils) {
        exclusions.add("bean://consulNodeDiscovery");
        exclusions.add("timer://consul-inspect");
        this.httpUtils = httpUtils;
    }

    @Override
    public RoutePolicy createRoutePolicy(CamelContext camelContext, String routeId, NamedNode route) {
        init(camelContext);
        return new CapiTracerRoutePolicy();
    }

    public void init(CamelContext camelContext) {
        if (!camelContext.hasService(this)) {
            try {
                camelContext.addService(this, true, true);
            } catch (Exception e) {
                throw RuntimeCamelException.wrapRuntimeCamelException(e);
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

    @ManagedAttribute(description = "The POST URL for the traces v2 api.")
    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    @ManagedAttribute(description = "The port number for the remote trace scribe collector.")
    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setSpanReporter(Reporter<zipkin2.Span> spanReporter) {
        this.spanReporter = spanReporter;
    }

    public void addServerServiceMapping(String pattern, String serviceName) {
        serverServiceMappings.put(pattern, serviceName);
    }

    @ManagedAttribute(description = "Whether to include the Camel message body in the traces")
    public boolean isIncludeMessageBody() {
        return includeMessageBody;
    }

    @ManagedAttribute(description = "Whether to include the Camel message body in the traces")
    public void setIncludeMessageBody(boolean includeMessageBody) {
        this.includeMessageBody = includeMessageBody;
    }

    @ManagedAttribute(description = "Whether to include stream based Camel message bodies in the traces")
    public boolean isIncludeMessageBodyStreams() {
        return includeMessageBodyStreams;
    }

    @ManagedAttribute(description = "Whether to include stream based Camel message bodies in the traces")
    public void setIncludeMessageBodyStreams(boolean includeMessageBodyStreams) {
        this.includeMessageBodyStreams = includeMessageBodyStreams;
    }

    @Override
    protected void doInit() throws Exception {
        camelContext.getManagementStrategy().addEventNotifier(eventNotifier);

        ObjectHelper.notNull(camelContext, "CamelContext", this);
        ObjectHelper.notNull(spanReporter, "Reporter<zipkin2.Span>", this);
        if (!camelContext.getRoutePolicyFactories().contains(this)) {
            camelContext.addRoutePolicyFactory(this);
        }
    }

    @Override
    protected void doShutdown() {
        ServiceHelper.stopAndShutdownService(spanReporter);
        if (spanReporter instanceof Closeable) {
            IOHelper.close((Closeable) spanReporter);
        }
        braves.clear();
        camelContext.getRoutePolicyFactories().remove(this);
    }

    private String getServiceName(Exchange exchange, Endpoint endpoint) {
        String serviceName = null;
        if (endpoint != null) {
            serviceName = endpoint.getEndpointKey();
        } else if (exchange.getFromEndpoint() != null) {
            serviceName = exchange.getFromEndpoint().getEndpointKey();
        }
        String sanitizedServiceName = sanitizeUri(serviceName);
        if (sanitizedServiceName != null) {
            if(sanitizedServiceName.startsWith(REST_ROUTE)) {
                sanitizedServiceName = normalizeServiceName(sanitizedServiceName);
                LOG.trace("Using serviceName: {}", sanitizedServiceName);
            }
        }
        return sanitizedServiceName;
    }

    private Tracing newTracing(String serviceName) {
        Tracing brave;
        float rate = 1.0f;
        if (camelContext.isUseMDCLogging()) {
            brave = Tracing.newBuilder()
                    .currentTraceContext(
                            ThreadLocalCurrentTraceContext.newBuilder().addScopeDecorator(MDCScopeDecorator.get()).build())
                    .localServiceName(serviceName).sampler(Sampler.create(rate)).addSpanHandler(ZipkinSpanHandler.create(spanReporter)).build();
        } else {
            brave = Tracing.newBuilder().localServiceName(serviceName).sampler(Sampler.create(rate)).addSpanHandler(ZipkinSpanHandler.create(spanReporter))
                    .build();
        }
        return brave;
    }

    private Tracing getTracing(String serviceName) {
        Tracing brave = null;
        if (serviceName != null) {
            brave = braves.get(serviceName);
            if (brave == null) {
                LOG.debug("Creating Tracing assigned to serviceName: {}", serviceName);
                brave = newTracing(serviceName);
                braves.put(serviceName, brave);
            }
        }
        return brave;
    }

    private void serverRequest(Tracing brave, Exchange exchange) {
        ExchangeExtension extendedExchange = exchange.getExchangeExtension();
        ZipkinState state = extendedExchange.getSafeCopyProperty(ZipkinState.KEY, ZipkinState.class);
        if (state == null) {
            state = new ZipkinState();
            extendedExchange.setSafeCopyProperty(ZipkinState.KEY, state);
        }
        Span span;
        CamelRequest camelRequest = new CamelRequest(exchange.getIn(), Span.Kind.SERVER);
        TraceContextOrSamplingFlags sampleFlag = EXTRACTOR.extract(camelRequest);
        if (ObjectHelper.isEmpty(sampleFlag)) {
            span = brave.tracer().nextSpan();
        } else {
            span = brave.tracer().nextSpan(sampleFlag);
        }
        span.kind(Span.Kind.SERVER).start();
        CapiTracerServerRequestAdapter parser = new CapiTracerServerRequestAdapter(exchange, this);
        parser.onRequest(exchange, span.customizer());
        INJECTOR.inject(span.context(), camelRequest);

        state.pushServerSpan(span);
        TraceContext context = span.context();
        String traceId = context.traceIdString();
        String spanId = String.valueOf(context.spanId());
        String parentId = context.parentId() != null ? String.valueOf(context.parentId()) : null;
        if (camelContext.isUseMDCLogging()) {
            MDC.put("traceId", traceId);
            MDC.put("spanId", spanId);
            MDC.put("parentId", parentId);
        }
    }

    private void serverResponse(String serviceName, Exchange exchange) {
        if(!exchange.getFromRouteId().startsWith("timer://")) {
            Span span = null;
            ExchangeExtension extendedExchange = exchange.getExchangeExtension();
            ZipkinState state = extendedExchange.getSafeCopyProperty(ZipkinState.KEY, ZipkinState.class);
            if (state != null) {
                span = state.popServerSpan();
            }

            if (span != null) {
                CapiTracerServerResponseAdapter parser = new CapiTracerServerResponseAdapter(serviceName);
                parser.onResponse(exchange, span.customizer());
                span.finish();
                TraceContext context = span.context();
                String traceId = context.traceIdString();
                String spanId = Long.toString(context.spanId());
                String parentId = context.parentId() != null ? Long.toString(context.parentId()) : null;
                if (camelContext.isUseMDCLogging()) {
                    MDC.put("traceId", traceId);
                    MDC.put("spanId", spanId);
                    MDC.put("parentId", parentId);
                }
            }
        }
    }

    private final class CapiTracerRoutePolicy extends RoutePolicySupport {

        @Override
        public void onExchangeBegin(Route route, Exchange exchange) {
            String serviceName = getServiceName(exchange, route.getEndpoint());
            Tracing brave = getTracing(serviceName);
            if (brave != null && isIncluded(serviceName)) {
                LOG.trace("Exchange BEGIN: " + serviceName);
                serverRequest(brave, exchange);
            }
        }

        @Override
        public void onExchangeDone(Route route, Exchange exchange) {
            String serviceName = getServiceName(exchange, route.getEndpoint());
            Tracing brave = getTracing(serviceName);
            if (brave != null && isIncluded(serviceName)) {
                LOG.trace("Exchange DONE: " + serviceName);
                serverResponse(serviceName, exchange);
            }
        }
    }

    public HttpUtils getHttpUtils() {
        return httpUtils;
    }

    private boolean isIncluded(String resource) {
        for(String exclusion : exclusions) {
            if(resource.startsWith(exclusion)) {
                return false;
            }
        }
        return true;
    }

    private String normalizeServiceName(String key) {
        key = key.replaceAll("rest://", "");
        String[] keyParts = key.split(":");
        if(keyParts.length > 1) {
            key = keyParts[1];
        }
        key = key.replaceAll("/", ":");
        key = "capi" + key;
        return key;
    }

    private static final class CapiEventNotifier extends EventNotifierSupport {
        @Override
        public void notify(CamelEvent camelEvent) throws Exception {
            if (camelEvent instanceof CamelEvent.ExchangeSendingEvent exchangeSendingEvent) {
                LOG.trace("NOTIFY CamelEvent.ExchangeSendingEvent");
                Endpoint endpoint = exchangeSendingEvent.getEndpoint();
                exchangeSendingEvent.getExchange().setProperty(Constants.CLIENT_START_TIME, System.currentTimeMillis());
                exchangeSendingEvent.getExchange().setProperty(Constants.CLIENT_ENDPOINT, endpoint.getEndpointKey());
            } else if (camelEvent instanceof CamelEvent.ExchangeSentEvent exchangeSentEvent) {
                LOG.trace("NOTIFY CamelEvent.ExchangeSentEvent");
                exchangeSentEvent.getExchange().setProperty(Constants.CLIENT_RESPONSE_CODE, exchangeSentEvent.getExchange().getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE, String.class));
                exchangeSentEvent.getExchange().setProperty(Constants.CLIENT_END_TIME, System.currentTimeMillis());
            }
        }

        @Override
        public boolean isEnabled(CamelEvent event) {
            return switch (event.getType()) {
                case ExchangeSending, ExchangeSent, ExchangeCreated, ExchangeCompleted, ExchangeFailed -> true;
                default -> false;
            };
        }

        @Override
        public String toString() {
            return "CapiEventNotifier";
        }
    }
}