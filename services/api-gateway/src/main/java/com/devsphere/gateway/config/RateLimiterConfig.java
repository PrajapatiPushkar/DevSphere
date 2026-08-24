package com.devsphere.gateway.config;

import java.net.InetSocketAddress;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimiterConfig {

    private final RateLimiterConfigProperties properties;

    public RateLimiterConfig(RateLimiterConfigProperties properties) {
        this.properties = properties;
    }

    @Bean("userKeyResolver")
    @Primary
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-Authenticated-User-Id");
            if (userId != null && !userId.isBlank()) {
                return Mono.just("rate_limit:user:" + userId);
            }
            return Mono.just("rate_limit:ip:" + resolveClientIp(exchange));
        };
    }

    @Bean("ipKeyResolver")
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just("rate_limit:ip:" + resolveClientIp(exchange));
    }

    @Bean("authRegisterRateLimiter")
    public RedisRateLimiter authRegisterRateLimiter() {
        return new RedisRateLimiter(
                properties.getRegistration().getReplenishRate(),
                properties.getRegistration().getBurstCapacity()
        );
    }

    @Bean("authLoginRateLimiter")
    public RedisRateLimiter authLoginRateLimiter() {
        return new RedisRateLimiter(
                properties.getLogin().getReplenishRate(),
                properties.getLogin().getBurstCapacity()
        );
    }

    @Bean("authenticatedUserRateLimiter")
    @Primary
    public RedisRateLimiter authenticatedUserRateLimiter() {
        return new RedisRateLimiter(
                properties.getAuthenticated().getReplenishRate(),
                properties.getAuthenticated().getBurstCapacity()
        );
    }

    @Bean("publicDefaultRateLimiter")
    public RedisRateLimiter publicDefaultRateLimiter() {
        return new RedisRateLimiter(
                properties.getPublicDefault().getReplenishRate(),
                properties.getPublicDefault().getBurstCapacity()
        );
    }

    public static String resolveClientIp(ServerWebExchange exchange) {
        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        if (remoteAddress != null && remoteAddress.getAddress() != null) {
            String hostAddress = remoteAddress.getAddress().getHostAddress();
            if (hostAddress != null && !hostAddress.isBlank()) {
                return hostAddress;
            }
        }
        return "127.0.0.1";
    }
}
