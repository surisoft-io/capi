package io.surisoft.capi.tracer;

import io.opentelemetry.api.trace.Span;
import org.apache.camel.SafeCopyProperty;

import java.util.ArrayDeque;
import java.util.Deque;

public class CapiTracingState implements SafeCopyProperty {
    public static final String KEY = "CapiTracingState";

    private final Deque<Span> serverSpans = new ArrayDeque<>();

    public void pushServerSpan(Span span) {
        serverSpans.push(span);
    }

    public Span popServerSpan() {
        if (serverSpans.isEmpty()) {
            return null;
        }
        return serverSpans.pop();
    }

    @Override
    public SafeCopyProperty safeCopy() {
        CapiTracingState copy = new CapiTracingState();
        copy.serverSpans.addAll(this.serverSpans);
        return copy;
    }
}