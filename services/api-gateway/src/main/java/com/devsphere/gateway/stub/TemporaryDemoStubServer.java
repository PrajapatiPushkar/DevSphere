package com.devsphere.gateway.stub;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

/**
 * TEMPORARY LESSON 3 VERIFICATION STUB
 * 
 * This stub server runs on port 8081 to act as a temporary downstream HTTP target
 * for testing Spring Cloud Gateway routing (/api/demo/** -> http://localhost:8081/internal/demo/**).
 * 
 * THIS IS NOT A REAL BUSINESS MICROSERVICE. It exists strictly for Lesson 3 routing verification
 * and will be removed once real business microservices (Auth, Task, User, etc.) are introduced.
 */
@Configuration
public class TemporaryDemoStubServer {

    private DisposableServer server;

    @PostConstruct
    public void start() {
        server = HttpServer.create()
                .port(8081)
                .handle((request, response) -> {
                    if (request.uri().startsWith("/internal/demo")) {
                        String jsonResponse = """
                                {
                                  "service": "temporary-demo-service",
                                  "message": "Request successfully routed through DevSphere API Gateway"
                                }
                                """;
                        return response.header("Content-Type", "application/json")
                                .sendString(Mono.just(jsonResponse));
                    }
                    return response.status(404).send();
                })
                .bindNow();
    }

    @PreDestroy
    public void stop() {
        if (server != null) {
            server.disposeNow();
        }
    }
}
