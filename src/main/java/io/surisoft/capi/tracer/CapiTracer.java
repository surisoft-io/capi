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
import org.apache.camel.support.*;
import org.apache.camel.support.service.ServiceHelper;
import org.apache.camel.support.service.ServiceSupport;
import org.apache.camel.util.IOHelper;
import org.apache.camel.util.ObjectHelper;
import org.apache.camel.util.URISupport;
import org.apache.camel.zipkin.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.Reporter;
import zipkin2.reporter.brave.ZipkinSpanHandler;
import zipkin2.reporter.libthrift.LibthriftSender;
import zipkin2.reporter.urlconnection.URLConnectionSender;

import java.io.Closeable;
import java.util.*;

import static org.apache.camel.util.URISupport.sanitizeUri;

@ManagedResource(description = "CapiTracer")
public class CapiTracer extends ServiceSupport implements RoutePolicyFactory, StaticService, CamelContextAware {
    private final HttpUtils httpUtils;

    //CAPI Only cares about rest routes
    private static final String REST_ROUTE = "rest://";

    private static final Logger LOG = LoggerFactory.getLogger(CapiTracer.class);
    private static final String TRACES_COLLECTOR_HTTP_SERVICE = "traces-collector-http";
    private static final String TRACES_COLLECTOR_THRIFT_SERVICE = "traces-collector-thrift";
    private static final Propagation.Getter<CamelRequest, String> GETTER = CamelRequest::getHeader;
    private static final Propagation.Setter<CamelRequest, String> SETTER = CamelRequest::setHeader;
    static final TraceContext.Extractor<CamelRequest> EXTRACTOR = B3Propagation.B3_STRING.extractor(GETTER);
    private static final TraceContext.Injector<CamelRequest> INJECTOR = B3Propagation.B3_STRING.injector(SETTER);
    private final CapiEventNotifier eventNotifier = new CapiEventNotifier();
    private final Map<String, Tracing> braves = new HashMap<>();
    private transient boolean useFallbackServiceNames;
    private CamelContext camelContext;
    private String endpoint;
    private int port;
    private Reporter spanReporter;
    private final Map<String, String> clientServiceMappings = new HashMap<>();
    private final Map<String, String> serverServiceMappings = new HashMap<>();
    private Set<String> excludePatterns = new HashSet<>();
    private boolean includeMessageBody;
    private boolean includeMessageBodyStreams;
    private final Map<String, Span.Kind> producerComponentToSpanKind = new HashMap<>();
    private final Map<String, Span.Kind> consumerComponentToSpanKind = new HashMap<>();
    private final List<String> exclusions = new ArrayList<>();

    public CapiTracer(HttpUtils httpUtils) {
        exclusions.add("bean://consulNodeDiscovery");
        exclusions.add("timer://consul-inspect");
        this.httpUtils = httpUtils;
        producerComponentToSpanKind.put("jms", Span.Kind.PRODUCER);
        producerComponentToSpanKind.put("sjms", Span.Kind.PRODUCER);
        producerComponentToSpanKind.put("activemq", Span.Kind.PRODUCER);
        producerComponentToSpanKind.put("kafka", Span.Kind.PRODUCER);
        producerComponentToSpanKind.put("amqp", Span.Kind.PRODUCER);
        consumerComponentToSpanKind.put("jms", Span.Kind.CONSUMER);
        consumerComponentToSpanKind.put("sjms", Span.Kind.CONSUMER);
        consumerComponentToSpanKind.put("activemq", Span.Kind.CONSUMER);
        consumerComponentToSpanKind.put("kafka", Span.Kind.CONSUMER);
        consumerComponentToSpanKind.put("amqp", Span.Kind.CONSUMER);
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

    /**
     * Sets the reporter used to send timing data (spans) to the trace server.
     */
    public void setSpanReporter(Reporter<zipkin2.Span> spanReporter) {
        this.spanReporter = spanReporter;
    }

    /**
     * Adds a server service mapping that matches Camel events to the given traces service name. See more details at the
     * class javadoc.
     *
     * @param pattern     the pattern such as route id, endpoint url
     * @param serviceName the traces service name
     */
    public void addServerServiceMapping(String pattern, String serviceName) {
        serverServiceMappings.put(pattern, serviceName);
    }

    public void setExcludePatterns(Set<String> excludePatterns) {
        this.excludePatterns = excludePatterns;
    }

    @ManagedAttribute(description = "Whether to include the Camel message body in the traces")
    public boolean isIncludeMessageBody() {
        return includeMessageBody;
    }

    /**
     * Whether to include the Camel message body in the traces.
     * <p/>
     * This is not recommended for production usage, or when having big payloads. You can limit the size by configuring
     * the <a href= "http://camel.apache.org/how-do-i-set-the-max-chars-when-debug-logging-messages-in-camel.html">max
     * debug log size</a>.
     * <p/>
     * By default message bodies that are stream based are <b>not</b> included. You can use the option
     * {@link #setIncludeMessageBodyStreams(boolean)} to turn that on.
     */
    @ManagedAttribute(description = "Whether to include the Camel message body in the traces")
    public void setIncludeMessageBody(boolean includeMessageBody) {
        this.includeMessageBody = includeMessageBody;
    }

    @ManagedAttribute(description = "Whether to include stream based Camel message bodies in the traces")
    public boolean isIncludeMessageBodyStreams() {
        return includeMessageBodyStreams;
    }

    /**
     * Whether to include message bodies that are stream based in the traces.
     * <p/>
     * This requires enabling <a href="http://camel.apache.org/stream-caching.html">stream caching</a> on the routes or
     * globally on the CamelContext.
     * <p/>
     * This is not recommended for production usage, or when having big payloads. You can limit the size by configuring
     * the <a href= "http://camel.apache.org/how-do-i-set-the-max-chars-when-debug-logging-messages-in-camel.html">max
     * debug log size</a>.
     */
    @ManagedAttribute(description = "Whether to include stream based Camel message bodies in the traces")
    public void setIncludeMessageBodyStreams(boolean includeMessageBodyStreams) {
        this.includeMessageBodyStreams = includeMessageBodyStreams;
    }

    @Override
    protected void doInit() throws Exception {
        ObjectHelper.notNull(camelContext, "CamelContext", this);

        camelContext.getManagementStrategy().addEventNotifier(eventNotifier);
        if (!camelContext.getRoutePolicyFactories().contains(this)) {
            camelContext.addRoutePolicyFactory(this);
        }

        if (spanReporter == null) {
            if (endpoint != null) {
                LOG.info("Configuring Traces URLConnectionSender using endpoint: {}", endpoint);
                spanReporter = AsyncReporter.create(URLConnectionSender.create(endpoint));
            } else {
                String host = ServiceHostFunction.apply(TRACES_COLLECTOR_HTTP_SERVICE);
                String port = ServicePortFunction.apply(TRACES_COLLECTOR_HTTP_SERVICE);
                if (ObjectHelper.isNotEmpty(host) && ObjectHelper.isNotEmpty(port)) {
                    LOG.trace("Auto-configuring Traces URLConnectionSender using host: {} and port: {}", host, port);
                    int num = camelContext.getTypeConverter().mandatoryConvertTo(Integer.class, port);
                    String implicitEndpoint = "http://" + host + ":" + num + "/api/v2/spans";
                    spanReporter = AsyncReporter.create(URLConnectionSender.create(implicitEndpoint));
                } else {
                    host = ServiceHostFunction.apply(TRACES_COLLECTOR_THRIFT_SERVICE);
                    port = ServicePortFunction.apply(TRACES_COLLECTOR_THRIFT_SERVICE);
                    if (ObjectHelper.isNotEmpty(host) && ObjectHelper.isNotEmpty(port)) {
                        LOG.trace("Auto-configuring Traces ScribeSpanCollector using host: {} and port: {}", host, port);
                        int num = camelContext.getTypeConverter().mandatoryConvertTo(Integer.class, port);
                        LibthriftSender sender = LibthriftSender.newBuilder().host(host).port(num).build();
                        spanReporter = AsyncReporter.create(sender);
                    }
                }
            }
        }

        if (spanReporter == null) {
            Set<Reporter> reporters = camelContext.getRegistry().findByType(Reporter.class);
            if (reporters.size() == 1) {
                spanReporter = reporters.iterator().next();
            }
        }

        ObjectHelper.notNull(spanReporter, "Reporter<zipkin2.Span>", this);

        if (clientServiceMappings.isEmpty() && serverServiceMappings.isEmpty()) {
            LOG.warn(
                    "No service name(s) has been mapped in clientServiceMappings or serverServiceMappings. Camel will fallback and use endpoint uris as service names.");
            useFallbackServiceNames = true;
        }

        // create braves mapped per service name
        for (Map.Entry<String, String> entry : clientServiceMappings.entrySet()) {
            String pattern = entry.getKey();
            String serviceName = entry.getValue();
            createTracingForService(pattern, serviceName);
        }
        for (Map.Entry<String, String> entry : serverServiceMappings.entrySet()) {
            String pattern = entry.getKey();
            String serviceName = entry.getValue();
            createTracingForService(pattern, serviceName);
        }

        ServiceHelper.startService(spanReporter, eventNotifier);
    }

    @Override
    protected void doShutdown() throws Exception {
        // stop event notifier
        camelContext.getManagementStrategy().removeEventNotifier(eventNotifier);
        ServiceHelper.stopService(eventNotifier);

        // stop and close collector
        ServiceHelper.stopAndShutdownService(spanReporter);
        if (spanReporter instanceof Closeable) {
            IOHelper.close((Closeable) spanReporter);
        }
        // clear braves
        braves.clear();
        // remove route policy
        camelContext.getRoutePolicyFactories().remove(this);
    }

    private String getServiceName(Exchange exchange, Endpoint endpoint, boolean server, boolean client) {
        if (client) {
            return getServiceName(exchange, endpoint);
        } else if (server) {
            return getServiceName(exchange, endpoint);
        } else {
            return null;
        }
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
                sanitizedServiceName = normalizeServiceName(sanitizedServiceName, false);
                LOG.trace("Using serviceName: {}", sanitizedServiceName);
            }
            if(sanitizedServiceName.startsWith("http://") || sanitizedServiceName.startsWith("https://")) {
                String httpUri = exchange.getIn().getHeader("CamelHttpUri", String.class);
                sanitizedServiceName = normalizeServiceName(httpUri, true);
                LOG.trace("Using serviceName: {}", sanitizedServiceName);
            }
        }
        return sanitizedServiceName;
    }

    private void createTracingForService(String pattern, String serviceName) {
        Tracing brave = braves.get(pattern);
        if (brave == null && !braves.containsKey(serviceName)) {
            brave = newTracing(serviceName);
            braves.put(serviceName, brave);
        }
    }

    private Tracing newTracing(String serviceName) {
        Tracing brave = null;
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

    private void clientRequest(Tracing brave, CamelEvent.ExchangeSendingEvent event) {
        // reuse existing span if we do multiple requests from the same
        ExchangeExtension exchange = event.getExchange().getExchangeExtension();
        ZipkinState state = exchange.getSafeCopyProperty(ZipkinState.KEY, ZipkinState.class);
        if (state == null) {
            state = new ZipkinState();
            exchange.setSafeCopyProperty(ZipkinState.KEY, state);
        }
        // if we started from a server span then lets reuse that when we call a
        // downstream service
        Span last = state.peekServerSpan();
        Span span;
        if (last != null) {
            span = brave.tracer().newChild(last.context());
        } else {
            span = brave.tracer().nextSpan();
        }

        Span.Kind spanKind = getProducerComponentSpanKind(event.getEndpoint());
        span.kind(spanKind).start();

        CapiTracerClientRequestAdapter parser = new CapiTracerClientRequestAdapter(event.getEndpoint(), normalizeClientEndpoint(event.getEndpoint()));
        CamelRequest request = new CamelRequest(event.getExchange().getIn(), spanKind);
        INJECTOR.inject(span.context(), request);
        parser.onRequest(event.getExchange(), span.customizer());

        // store span after request
        state.pushClientSpan(span);
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

    protected Span.Kind getProducerComponentSpanKind(Endpoint endpoint) {
        return producerComponentToSpanKind.getOrDefault(getComponentName(endpoint), Span.Kind.CLIENT);
    }

    private void clientResponse(CamelEvent.ExchangeSentEvent event) {
        Span span = null;
        ExchangeExtension exchange = event.getExchange().getExchangeExtension(); //.adapt(ExtendedExchange.class);
        ZipkinState state = exchange.getSafeCopyProperty(ZipkinState.KEY, ZipkinState.class);
        if (state != null) {
            // only process if it was a zipkin client event
            span = state.popClientSpan();
        }

        if (span != null) {
            CapiTracerClientResponseAdapter parser = new CapiTracerClientResponseAdapter(this);
            parser.onResponse(event.getExchange(), span.customizer());
            span.finish();
            TraceContext context = span.context();
            String traceId = context.traceIdString();
            String spanId = Long.toString(context.spanId());
            String parentId = context.parentId() != null ? String.valueOf(context.parentId()) : null;
            if (camelContext.isUseMDCLogging()) {
                MDC.put("traceId", traceId);
                MDC.put("spanId", spanId);
                MDC.put("parentId", parentId);
            }
        }
    }

    private Span serverRequest(Tracing brave, Exchange exchange, String serviceName) {
        // reuse existing span if we do multiple requests from the same
        ExchangeExtension extendedExchange = exchange.getExchangeExtension(); //.adapt(ExtendedExchange.class);
        ZipkinState state = extendedExchange.getSafeCopyProperty(ZipkinState.KEY, ZipkinState.class);
        if (state == null) {
            state = new ZipkinState();
            extendedExchange.setSafeCopyProperty(ZipkinState.KEY, state);
        }
        Span span = null;
        Span.Kind spanKind = getConsumerComponentSpanKind(exchange.getFromEndpoint());
        CamelRequest camelRequest = new CamelRequest(exchange.getIn(), spanKind);
        TraceContextOrSamplingFlags sampleFlag = EXTRACTOR.extract(camelRequest);
        if (ObjectHelper.isEmpty(sampleFlag)) {
            span = brave.tracer().nextSpan();
        } else {
            span = brave.tracer().nextSpan(sampleFlag);
        }
        span.kind(spanKind).start();
        CapiTracerServerRequestAdapter parser = new CapiTracerServerRequestAdapter(exchange, serviceName, this);
        parser.onRequest(exchange, span.customizer());
        INJECTOR.inject(span.context(), camelRequest);

        // store span after request
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
        return span;
    }

    //protected for testing
    protected Span.Kind getConsumerComponentSpanKind(Endpoint endpoint) {
        return consumerComponentToSpanKind.getOrDefault(getComponentName(endpoint), Span.Kind.SERVER);
    }

    private void serverResponse(String serviceName, Exchange exchange) {

        if(!exchange.getFromRouteId().startsWith("timer://")) {
            Span span = null;
            ExchangeExtension extendedExchange = exchange.getExchangeExtension();
            ZipkinState state = extendedExchange.getSafeCopyProperty(ZipkinState.KEY, ZipkinState.class);
            if (state != null) {
                // only process if it was a zipkin server event
                span = state.popServerSpan();
            }

            if (span != null) {
                CapiTracerServerResponseAdapter parser = new CapiTracerServerResponseAdapter(this, serviceName);
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

    private String getComponentName(Endpoint endpoint) {
        String uri = endpoint.getEndpointBaseUri();
        if (uri != null) {
            String[] uriParts = uri.split(":");
            if (uriParts.length > 0) {
                return uriParts[0].toLowerCase();
            }
        }
        return null;
    }

    private final class CapiEventNotifier extends EventNotifierSupport {

        @Override
        public void notify(CamelEvent camelEvent) throws Exception {
            // use event notifier to track events when Camel messages to
            // endpoints
            // these events corresponds to Tracer client events

            // client events
            if (camelEvent instanceof CamelEvent.ExchangeSendingEvent exchangeSendingEvent) {
                LOG.trace("NOTIFY CamelEvent.ExchangeSendingEvent");
                String serviceName = getServiceName(exchangeSendingEvent.getExchange(), exchangeSendingEvent.getEndpoint(), false, true);
                Tracing brave = getTracing(serviceName);
                if (brave != null && isIncluded(serviceName)) {
                    clientRequest(brave, exchangeSendingEvent);
                }
            } else if (camelEvent instanceof CamelEvent.ExchangeSentEvent exchangeSentEvent) {
                LOG.trace("NOTIFY CamelEvent.ExchangeSentEvent");
                String serviceName = getServiceName(exchangeSentEvent.getExchange(), exchangeSentEvent.getEndpoint(), false, true);
                Tracing brave = getTracing(serviceName);
                if (brave != null && isIncluded(serviceName)) {
                    clientResponse(exchangeSentEvent);
                }
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

    private final class CapiTracerRoutePolicy extends RoutePolicySupport {

        @Override
        public void onExchangeBegin(Route route, Exchange exchange) {
            // use route policy to track events when Camel a Camel route
            // begins/end the lifecycle of an Exchange
            // these events corresponds to Zipkin server events

            String serviceName = getServiceName(exchange, route.getEndpoint(), true, false);
            Tracing brave = getTracing(serviceName);
            if (brave != null && isIncluded(serviceName)) {
                LOG.trace("Exchange BEGIN: " + serviceName);
                serverRequest(brave, exchange, serviceName);
            }

        }

        // Report Server send after route has completed processing of the exchange.
        @Override
        public void onExchangeDone(Route route, Exchange exchange) {
            String serviceName = getServiceName(exchange, route.getEndpoint(), true, false);
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

    private String normalizeServiceName(String key, boolean isUri) {
        if(isUri) {
            String[] keyParts = key.split("/");
            key = keyParts[1] + "-forward" + ":" + keyParts[2] + ":" + keyParts[3];
            return key;
        } else {
            key = key.replaceAll("rest://", "");
            String[] keyParts = key.split(":");
            if(keyParts.length > 1) {
                key = keyParts[1];
            } /*else {
            return key;
        }*/
            key = key.replaceAll("/", ":");
            key = "capi" + key;
            return key;
        }
    }

    private String normalizeClientEndpoint(Endpoint endpoint) {
        String sanitizeEndpoint = URISupport.sanitizeUri(endpoint.getEndpointUri());
        if(sanitizeEndpoint.contains("?")) {
            return sanitizeEndpoint.substring(0, sanitizeEndpoint.indexOf("?"));
        }
        return sanitizeEndpoint;
    }
}