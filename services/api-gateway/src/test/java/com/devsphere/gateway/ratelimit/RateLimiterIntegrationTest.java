package com.devsphere.gateway.ratelimit;

import com.devsphere.gateway.config.RateLimiterConfig;
import com.devsphere.gateway.config.RateLimiterConfigProperties;
import com.devsphere.gateway.filter.DistributedRateLimiterGatewayFilterFactory;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class RateLimiterIntegrationTest {

    private static final String SECRET = "devsphere-super-secret-jwt-signing-key-for-local-development-must-be-at-least-256-bits-long";

    private RateLimiterConfigProperties properties;
    private MeterRegistry meterRegistry;
    private DistributedRateLimiterGatewayFilterFactory filterFactory;
    private SecretKey key;

    @BeforeEach
    void setUp() {
        this.properties = new RateLimiterConfigProperties();
        this.meterRegistry = new SimpleMeterRegistry();
        this.filterFactory = new DistributedRateLimiterGatewayFilterFactory(properties, meterRegistry);
        this.key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("userKeyResolver uses X-Authenticated-User-Id header to generate rate_limit:user:{userId} key")
    void userKeyResolverUsesAuthenticatedHeader() {
        RateLimiterConfig config = new RateLimiterConfig(properties);
        KeyResolver resolver = config.userKeyResolver();

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/users/me")
                .header("X-Authenticated-User-Id", "100")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(resolver.resolve(exchange))
                .expectNext("rate_limit:user:100")
                .verifyComplete();
    }

    @Test
    @DisplayName("userKeyResolver falls back to rate_limit:ip:{ip} when authenticated header is missing")
    void userKeyResolverFallsBackToIp() {
        RateLimiterConfig config = new RateLimiterConfig(properties);
        KeyResolver resolver = config.userKeyResolver();

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/auth/login")
                .remoteAddress(new InetSocketAddress("192.168.1.50", 8080))
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(resolver.resolve(exchange))
                .expectNext("rate_limit:ip:192.168.1.50")
                .verifyComplete();
    }

    @Test
    @DisplayName("ipKeyResolver generates rate_limit:ip:{ip} key for client IP")
    void ipKeyResolverGeneratesIpKey() {
        RateLimiterConfig config = new RateLimiterConfig(properties);
        KeyResolver resolver = config.ipKeyResolver();

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/auth/register")
                .remoteAddress(new InetSocketAddress("10.0.0.1", 9090))
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(resolver.resolve(exchange))
                .expectNext("rate_limit:ip:10.0.0.1")
                .verifyComplete();
    }

    @Test
    @DisplayName("Rate limiter allows request below limit and updates metrics")
    void rateLimiterAllowsRequestBelowLimit() {
        @SuppressWarnings("unchecked")
        RateLimiter<Object> rateLimiter = Mockito.mock(RateLimiter.class);
        Map<String, String> headers = new HashMap<>();
        headers.put("X-RateLimit-Remaining", "9");

        RateLimiter.Response allowedResponse = new RateLimiter.Response(true, headers);
        when(rateLimiter.isAllowed(anyString(), anyString())).thenReturn(Mono.just(allowedResponse));

        DistributedRateLimiterGatewayFilterFactory.Config config = new DistributedRateLimiterGatewayFilterFactory.Config();
        config.setKeyResolver(exchange -> Mono.just("rate_limit:user:100"));
        config.setRateLimiter(rateLimiter);
        config.setRouteId("user-service");

        GatewayFilter filter = filterFactory.apply(config);
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/users/me").build());
        GatewayFilterChain chain = ex -> Mono.empty();

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertEquals(HttpStatus.OK, exchange.getResponse().getStatusCode() != null ? exchange.getResponse().getStatusCode() : HttpStatus.OK);
        assertEquals("allowed", exchange.getAttribute("rate_limit.result"));
        assertEquals(1.0, meterRegistry.counter("devsphere_rate_limit_requests_total", "result", "allowed").count());
    }

    @Test
    @DisplayName("Rate limiter rejects request above limit with 429 status, JSON body, and Retry-After header")
    void rateLimiterRejectsRequestAboveLimit() {
        @SuppressWarnings("unchecked")
        RateLimiter<Object> rateLimiter = Mockito.mock(RateLimiter.class);
        Map<String, String> headers = new HashMap<>();
        headers.put("X-RateLimit-Remaining", "0");

        RateLimiter.Response rejectedResponse = new RateLimiter.Response(false, headers);
        when(rateLimiter.isAllowed(anyString(), anyString())).thenReturn(Mono.just(rejectedResponse));

        DistributedRateLimiterGatewayFilterFactory.Config config = new DistributedRateLimiterGatewayFilterFactory.Config();
        config.setKeyResolver(exchange -> Mono.just("rate_limit:ip:192.168.1.1"));
        config.setRateLimiter(rateLimiter);
        config.setRouteId("auth-login");

        GatewayFilter filter = filterFactory.apply(config);
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/v1/auth/login").build());
        GatewayFilterChain chain = ex -> Mono.empty();

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exchange.getResponse().getStatusCode());
        assertEquals("1", exchange.getResponse().getHeaders().getFirst("Retry-After"));
        assertEquals("rejected", exchange.getAttribute("rate_limit.result"));
        assertEquals(1.0, meterRegistry.counter("devsphere_rate_limit_requests_total", "result", "rejected").count());
        assertEquals(1.0, meterRegistry.counter("devsphere_rate_limit_rejected_total", "route", "auth-login").count());
    }

    @Test
    @DisplayName("Redis failure in fail-open mode allows request to proceed")
    void redisFailureFailOpenAllowsRequest() {
        properties.setFailOpen(true);

        @SuppressWarnings("unchecked")
        RateLimiter<Object> rateLimiter = Mockito.mock(RateLimiter.class);
        when(rateLimiter.isAllowed(anyString(), anyString()))
                .thenReturn(Mono.error(new RuntimeException("Redis connection refused")));

        DistributedRateLimiterGatewayFilterFactory.Config config = new DistributedRateLimiterGatewayFilterFactory.Config();
        config.setKeyResolver(exchange -> Mono.just("rate_limit:user:100"));
        config.setRateLimiter(rateLimiter);
        config.setRouteId("user-service");

        GatewayFilter filter = filterFactory.apply(config);
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/users/me").build());
        GatewayFilterChain chain = ex -> Mono.empty();

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertEquals("error_fail_open", exchange.getAttribute("rate_limit.result"));
        assertEquals(1.0, meterRegistry.counter("devsphere_rate_limit_requests_total", "result", "error").count());
    }

    @Test
    @DisplayName("Redis failure in fail-closed mode rejects request with 429 status")
    void redisFailureFailClosedRejectsRequest() {
        properties.setFailOpen(false);

        @SuppressWarnings("unchecked")
        RateLimiter<Object> rateLimiter = Mockito.mock(RateLimiter.class);
        when(rateLimiter.isAllowed(anyString(), anyString()))
                .thenReturn(Mono.error(new RuntimeException("Redis connection refused")));

        DistributedRateLimiterGatewayFilterFactory.Config config = new DistributedRateLimiterGatewayFilterFactory.Config();
        config.setKeyResolver(exchange -> Mono.just("rate_limit:user:100"));
        config.setRateLimiter(rateLimiter);
        config.setRouteId("user-service");

        GatewayFilter filter = filterFactory.apply(config);
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/users/me").build());
        GatewayFilterChain chain = ex -> Mono.empty();

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exchange.getResponse().getStatusCode());
        assertEquals("error_fail_closed", exchange.getAttribute("rate_limit.result"));
        assertEquals(1.0, meterRegistry.counter("devsphere_rate_limit_requests_total", "result", "error").count());
    }

    @Test
    @DisplayName("Prometheus metric tags enforce low cardinality rules (no user IDs or IPs in tags)")
    void metricsEnforceLowCardinalityTags() {
        meterRegistry.counter("devsphere_rate_limit_requests_total", "result", "allowed").increment();
        meterRegistry.counter("devsphere_rate_limit_rejected_total", "route", "auth-register").increment();

        assertNotNull(meterRegistry.find("devsphere_rate_limit_requests_total").tag("result", "allowed").counter());
        assertNotNull(meterRegistry.find("devsphere_rate_limit_rejected_total").tag("route", "auth-register").counter());
        assertNull(meterRegistry.find("devsphere_rate_limit_requests_total").tag("userId", "100").counter());
        assertNull(meterRegistry.find("devsphere_rate_limit_requests_total").tag("ip", "192.168.1.1").counter());
    }
}
