package com.devsphere.gateway.filter;

import io.micrometer.tracing.Tracer;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class TracePropagationGlobalFilter implements GlobalFilter, WebFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(TracePropagationGlobalFilter.class);
    public static final String TRACE_HEADER_NAME = "X-Trace-Id";
    public static final String W3C_TRACEPARENT_HEADER = "traceparent";

    private final Tracer tracer;

    public TracePropagationGlobalFilter() {
        this(null);
    }

    @Autowired
    public TracePropagationGlobalFilter(@Autowired(required = false) Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return processTrace(exchange, chain::filter);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return processTrace(exchange, chain::filter);
    }

    private Mono<Void> processTrace(ServerWebExchange exchange, java.util.function.Function<ServerWebExchange, Mono<Void>> next) {
        ServerHttpRequest request = exchange.getRequest();
        
        String traceId = request.getHeaders().getFirst(TRACE_HEADER_NAME);
        String traceparent = request.getHeaders().getFirst(W3C_TRACEPARENT_HEADER);

        if ((traceId == null || traceId.isBlank()) && (traceparent != null && !traceparent.isBlank())) {
            String[] parts = traceparent.split("-");
            if (parts.length >= 2 && !parts[1].isBlank()) {
                traceId = parts[1];
            }
        }

        if (traceId == null || traceId.isBlank()) {
            if (tracer != null && tracer.currentSpan() != null && tracer.currentSpan().context() != null) {
                traceId = tracer.currentSpan().context().traceId();
            }
        }

        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }

        final String finalTraceId = traceId;

        MDC.put("traceId", finalTraceId);

        ServerHttpRequest modifiedRequest = request.mutate()
                .header(TRACE_HEADER_NAME, finalTraceId)
                .build();

        if (!exchange.getResponse().getHeaders().containsKey(TRACE_HEADER_NAME)) {
            exchange.getResponse().getHeaders().add(TRACE_HEADER_NAME, finalTraceId);
        }

        log.debug("Propagating trace context ID: {} for path: {}", finalTraceId, request.getPath().value());

        return next.apply(exchange.mutate().request(modifiedRequest).build())
                .doFinally(signalType -> MDC.remove("traceId"));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
