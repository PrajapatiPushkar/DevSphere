package com.devsphere.gateway.filter;

import com.devsphere.gateway.config.RateLimiterConfigProperties;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.cloud.gateway.support.HasRouteId;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component("DistributedRateLimiter")
public class DistributedRateLimiterGatewayFilterFactory
        extends AbstractGatewayFilterFactory<DistributedRateLimiterGatewayFilterFactory.Config> {

    private static final Logger log = LoggerFactory.getLogger(DistributedRateLimiterGatewayFilterFactory.class);

    private final RateLimiterConfigProperties properties;
    private final MeterRegistry meterRegistry;

    public DistributedRateLimiterGatewayFilterFactory(RateLimiterConfigProperties properties) {
        this(properties, new SimpleMeterRegistry());
    }

    @org.springframework.beans.factory.annotation.Autowired
    public DistributedRateLimiterGatewayFilterFactory(RateLimiterConfigProperties properties, MeterRegistry meterRegistry) {
        super(Config.class);
        this.properties = properties;
        this.meterRegistry = meterRegistry != null ? meterRegistry : new SimpleMeterRegistry();
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            if (!properties.isEnabled()) {
                return chain.filter(exchange);
            }

            KeyResolver keyResolver = config.getKeyResolver();
            @SuppressWarnings("unchecked")
            RateLimiter<Object> rateLimiter = (RateLimiter<Object>) config.getRateLimiter();

            if (keyResolver == null || rateLimiter == null) {
                return chain.filter(exchange);
            }

            String routeId = config.getRouteId() != null ? config.getRouteId() : "default-route";

            return keyResolver.resolve(exchange)
                    .defaultIfEmpty("rate_limit:ip:127.0.0.1")
                    .flatMap(key -> {
                        long timeoutMs = properties.getTimeoutMs() > 0 ? properties.getTimeoutMs() : 2000;
                        return rateLimiter.isAllowed(routeId, key)
                                .timeout(Duration.ofMillis(timeoutMs))
                                .flatMap(limResponse -> {
                                    if (limResponse.getHeaders() != null) {
                                        for (Map.Entry<String, String> header : limResponse.getHeaders().entrySet()) {
                                            exchange.getResponse().getHeaders().add(header.getKey(), header.getValue());
                                        }
                                    }

                                    if (limResponse.isAllowed()) {
                                        meterRegistry.counter("devsphere_rate_limit_requests_total", "result", "allowed").increment();
                                        exchange.getAttributes().put("rate_limit.result", "allowed");
                                        return chain.filter(exchange);
                                    } else {
                                        meterRegistry.counter("devsphere_rate_limit_requests_total", "result", "rejected").increment();
                                        meterRegistry.counter("devsphere_rate_limit_rejected_total", "route", routeId).increment();
                                        exchange.getAttributes().put("rate_limit.result", "rejected");
                                        return onRateLimitExceeded(exchange);
                                    }
                                })
                                .onErrorResume((Throwable ex) -> {
                                    meterRegistry.counter("devsphere_rate_limit_requests_total", "result", "error").increment();
                                    if (properties.isFailOpen()) {
                                        log.warn("Redis rate limiter error or timeout on route '{}'. Failing open: {}", routeId, ex.getMessage());
                                        exchange.getAttributes().put("rate_limit.result", "error_fail_open");
                                        return chain.filter(exchange);
                                    } else {
                                        log.error("Redis rate limiter error or timeout on route '{}'. Failing closed: {}", routeId, ex.getMessage());
                                        exchange.getAttributes().put("rate_limit.result", "error_fail_closed");
                                        return onRateLimitExceeded(exchange);
                                    }
                                });
                    });
        };
    }

    private Mono<Void> onRateLimitExceeded(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().set("Retry-After", "1");

        String jsonBody = """
                {
                  "status": 429,
                  "error": "TOO_MANY_REQUESTS",
                  "message": "Rate limit exceeded. Please try again later."
                }
                """;

        DataBuffer buffer = response.bufferFactory().wrap(jsonBody.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    public static class Config implements HasRouteId {
        private KeyResolver keyResolver;
        private RateLimiter rateLimiter;
        private String routeId;

        public KeyResolver getKeyResolver() {
            return keyResolver;
        }

        public Config setKeyResolver(KeyResolver keyResolver) {
            this.keyResolver = keyResolver;
            return this;
        }

        public RateLimiter getRateLimiter() {
            return rateLimiter;
        }

        public Config setRateLimiter(RateLimiter rateLimiter) {
            this.rateLimiter = rateLimiter;
            return this;
        }

        @Override
        public String getRouteId() {
            return routeId;
        }

        @Override
        public void setRouteId(String routeId) {
            this.routeId = routeId;
        }
    }
}
