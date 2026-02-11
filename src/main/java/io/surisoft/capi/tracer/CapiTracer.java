package io.surisoft.capi.tracer;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.surisoft.capi.schema.Service;
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
import org.apache.camel.support.service.ServiceSupport;
import org.apache.camel.util.ObjectHelper;
import org.cache2k.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

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

    private static final TextMapGetter<Message> GETTER = new TextMapGetter<>() {
        @Override
        public Iterable<String> keys(Message carrier) {
            return carrier.getHeaders().keySet();
        }

        @Override
        public String get(Message carrier, String key) {
            return carrier.getHeader(key, String.class);
        }
    };

    private static final TextMapSetter<Message> SETTER = Message::setHeader;

    private final Tracer tracer;
    private final OpenTelemetry openTelemetry;
    private CamelContext camelContext;
    private String endpoint;
    private int port;
    private final Map<String, String> serverServiceMappings = new HashMap<>();
    private boolean includeMessageBody;
    private boolean includeMessageBodyStreams;
    private final List<String> exclusions = new ArrayList<>();
    private final Cache<String, Service> serviceCache;

    private final CapiEventNotifier eventNotifier = new CapiEventNotifier();
    private final String capiNamespace;

    public CapiTracer(HttpUtils httpUtils, String capiNamespace, Cache<String, Service> serviceCache, Tracer tracer, OpenTelemetry openTelemetry) {
        exclusions.add("bean://consulNodeDiscovery");
        exclusions.add("timer://consul-inspect");
        exclusions.add("bean://consistencyChecker");
        exclusions.add("timer://consistency-checker");
        exclusions.add("timer://consul-KV-Store");
        exclusions.add("kafka://capi");
        this.httpUtils = httpUtils;
        this.capiNamespace = capiNamespace;
        this.serviceCache = serviceCache;
        this.tracer = tracer;
        this.openTelemetry = openTelemetry;
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

    @ManagedAttribute(description = "The POST URL for the traces endpoint.")
    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    @ManagedAttribute(description = "The port number for the remote trace collector.")
    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void addServerServiceMapping(String pattern, String serviceName) {
        serverServiceMappings.put(pattern, serviceName);
    }

    public String getCapiNamespace() {
        return capiNamespace;
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
        ObjectHelper.notNull(tracer, "Tracer", this);
        if (!camelContext.getRoutePolicyFactories().contains(this)) {
            camelContext.addRoutePolicyFactory(this);
        }
    }

    @Override
    protected void doShutdown() {
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
            if (sanitizedServiceName.startsWith(REST_ROUTE)) {
                sanitizedServiceName = normalizeServiceName(sanitizedServiceName);
                LOG.trace("Using serviceName: {}", sanitizedServiceName);
            }
        }
        return sanitizedServiceName;
    }

    private void serverRequest(String serviceName, Exchange exchange) {
        ExchangeExtension extendedExchange = exchange.getExchangeExtension();
        CapiTracingState state = extendedExchange.getSafeCopyProperty(CapiTracingState.KEY, CapiTracingState.class);
        if (state == null) {
            state = new CapiTracingState();
            extendedExchange.setSafeCopyProperty(CapiTracingState.KEY, state);
        }

        Context extractedContext = openTelemetry.getPropagators().getTextMapPropagator()
                .extract(Context.current(), exchange.getIn(), GETTER);

        Span span = tracer.spanBuilder(serviceName != null ? serviceName : "unknown")
                .setParent(extractedContext)
                .setSpanKind(SpanKind.SERVER)
                .startSpan();

        CapiTracerServerRequestAdapter parser = new CapiTracerServerRequestAdapter(exchange, this, serviceCache);
        parser.onRequest(exchange, span);

        openTelemetry.getPropagators().getTextMapPropagator()
                .inject(Context.current().with(span), exchange.getIn(), SETTER);

        state.pushServerSpan(span);

        if (camelContext.isUseMDCLogging()) {
            MDC.put("traceId", span.getSpanContext().getTraceId());
            MDC.put("spanId", span.getSpanContext().getSpanId());
        }
    }

    private void serverResponse(String serviceName, Exchange exchange) {
        if (!exchange.getFromRouteId().startsWith("timer://")) {
            Span span = null;
            ExchangeExtension extendedExchange = exchange.getExchangeExtension();
            CapiTracingState state = extendedExchange.getSafeCopyProperty(CapiTracingState.KEY, CapiTracingState.class);
            if (state != null) {
                span = state.popServerSpan();
            }

            if (span != null) {
                CapiTracerServerResponseAdapter parser = new CapiTracerServerResponseAdapter(serviceName);
                parser.onResponse(exchange, span);
                span.end();

                if (camelContext.isUseMDCLogging()) {
                    MDC.put("traceId", span.getSpanContext().getTraceId());
                    MDC.put("spanId", span.getSpanContext().getSpanId());
                }
            }
        }
    }

    private final class CapiTracerRoutePolicy extends RoutePolicySupport {

        @Override
        public void onExchangeBegin(Route route, Exchange exchange) {
            String serviceName = getServiceName(exchange, route.getEndpoint());
            if (serviceName != null && isIncluded(serviceName)) {
                LOG.trace("Exchange BEGIN: " + serviceName);
                serverRequest(serviceName, exchange);
            }
        }

        @Override
        public void onExchangeDone(Route route, Exchange exchange) {
            String serviceName = getServiceName(exchange, route.getEndpoint());
            if (serviceName != null && isIncluded(serviceName)) {
                LOG.trace("Exchange DONE: " + serviceName);
                serverResponse(serviceName, exchange);
            }
        }
    }

    public HttpUtils getHttpUtils() {
        return httpUtils;
    }

    private boolean isIncluded(String resource) {
        for (String exclusion : exclusions) {
            if (resource.startsWith(exclusion)) {
                return false;
            }
        }
        return true;
    }

    private String normalizeServiceName(String key) {
        key = key.replaceAll("rest://", "");
        String[] keyParts = key.split(":");
        if (keyParts.length > 1) {
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
                Endpoint endpoint = exchangeSendingEvent.getEndpoint();
                exchangeSendingEvent.getExchange().setProperty(Constants.CLIENT_START_TIME, System.currentTimeMillis());
                exchangeSendingEvent.getExchange().setProperty(Constants.CLIENT_ENDPOINT, endpoint.getEndpointKey());
            } else if (camelEvent instanceof CamelEvent.ExchangeSentEvent exchangeSentEvent) {
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