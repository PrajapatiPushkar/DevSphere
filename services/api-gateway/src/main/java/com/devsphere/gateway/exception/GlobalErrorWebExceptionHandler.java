package com.devsphere.gateway.exception;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Order(-2)
public class GlobalErrorWebExceptionHandler implements ErrorWebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalErrorWebExceptionHandler.class);

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String code = "INTERNAL_SERVER_ERROR";
        String message = "An unexpected internal error occurred";

        if (ex instanceof ResponseStatusException rse) {
            HttpStatus resolved = HttpStatus.resolve(rse.getStatusCode().value());
            if (resolved != null) {
                status = resolved;
            }
            if (status == HttpStatus.NOT_FOUND) {
                code = "RESOURCE_NOT_FOUND";
                message = "Requested resource was not found";
            } else if (status == HttpStatus.METHOD_NOT_ALLOWED) {
                code = "METHOD_NOT_ALLOWED";
                message = "HTTP method is not supported for this endpoint";
            } else if (status == HttpStatus.UNAUTHORIZED) {
                code = "UNAUTHORIZED";
                message = "Authentication is required";
            } else if (status == HttpStatus.FORBIDDEN) {
                code = "FORBIDDEN";
                message = "You do not have permission to access this resource";
            } else {
                code = status.name();
                message = rse.getReason() != null ? rse.getReason() : "Request failed with status " + status.value();
            }
        }

        String path = exchange.getRequest().getURI().getPath();
        log.warn("Gateway error handled for path {}: status={}, code={}, message={}", path, status.value(), code, ex.getMessage());

        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String traceId = exchange.getRequest().getHeaders().getFirst("X-Trace-Id");
        if (traceId == null || traceId.isBlank()) {
            String traceparent = exchange.getRequest().getHeaders().getFirst("traceparent");
            if (traceparent != null && !traceparent.isBlank()) {
                String[] parts = traceparent.split("-");
                if (parts.length >= 2 && !parts[1].isBlank()) {
                    traceId = parts[1];
                }
            }
        }

        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"timestamp\": \"").append(Instant.now().toString()).append("\",\n");
        json.append("  \"status\": ").append(status.value()).append(",\n");
        json.append("  \"error\": \"").append(status.name()).append("\",\n");
        json.append("  \"code\": \"").append(code).append("\",\n");
        json.append("  \"message\": \"").append(message).append("\",\n");
        json.append("  \"path\": \"").append(path).append("\"");
        if (traceId != null && !traceId.isBlank()) {
            json.append(",\n  \"traceId\": \"").append(traceId).append("\"");
        }
        json.append("\n}");

        DataBuffer buffer = response.bufferFactory().wrap(json.toString().getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
