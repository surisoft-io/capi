package io.surisoft.capi.configuration;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.surisoft.capi.schema.Service;
import io.surisoft.capi.tracer.CapiTracer;
import io.surisoft.capi.tracer.CapiGatewayTracer;
import io.surisoft.capi.utils.HttpUtils;
import org.apache.camel.CamelContext;
import org.cache2k.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "capi.traces", name = "enabled", havingValue = "true")
public class CapiTracerConfiguration {

    private static final Logger log = LoggerFactory.getLogger(CapiTracerConfiguration.class);
    private final String tracesEndpoint;
    private final HttpUtils httpUtils;
    private final String capiNamespace;

    public CapiTracerConfiguration(@Value("${capi.traces.endpoint}") String tracesEndpoint,
                                   @Value("${capi.namespace}") String capiNamespace,
                                   HttpUtils httpUtils) {
        this.tracesEndpoint = tracesEndpoint;
        this.httpUtils = httpUtils;
        this.capiNamespace = capiNamespace;
    }

    @Bean
    public OpenTelemetry openTelemetry() {
        OtlpHttpSpanExporter exporter = OtlpHttpSpanExporter.builder()
                .setEndpoint(tracesEndpoint + "/v1/traces")
                .build();

        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(exporter))
                .setResource(Resource.getDefault().merge(
                        Resource.create(Attributes.of(
                                AttributeKey.stringKey("service.name"), "capi-gateway"))))
                .build();

        return OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .build();
    }

    @Bean
    public CapiTracer capiTracer(CamelContext camelContext, Cache<String, Service> serviceCache, OpenTelemetry openTelemetry) {
        camelContext.setUseMDCLogging(true);

        log.debug("Traces Enabled!");

        Tracer tracer = openTelemetry.getTracer("capi-gateway");
        CapiTracer capiTracer = new CapiTracer(httpUtils, capiNamespace, serviceCache, tracer, openTelemetry);
        capiTracer.setIncludeMessageBody(true);
        capiTracer.setIncludeMessageBodyStreams(true);

        capiTracer.init(camelContext);

        return capiTracer;
    }

    @Bean
    public CapiGatewayTracer capiGatewayTracer(OpenTelemetry openTelemetry) throws Exception {
        log.debug("Gateway Traces Enabled!");

        Tracer tracer = openTelemetry.getTracer("capi-gateway-proxy");
        CapiGatewayTracer capiGatewayTracer = new CapiGatewayTracer(httpUtils, tracer);
        capiGatewayTracer.init();
        return capiGatewayTracer;
    }
}