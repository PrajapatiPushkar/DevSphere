package com.devsphere.user.config;

import com.devsphere.user.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final MeterRegistry meterRegistry;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this(jwtAuthenticationFilter, new SimpleMeterRegistry());
    }

    @org.springframework.beans.factory.annotation.Autowired
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, MeterRegistry meterRegistry) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.meterRegistry = meterRegistry != null ? meterRegistry : new SimpleMeterRegistry();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(customAuthenticationEntryPoint())
                        .accessDeniedHandler(customAccessDeniedHandler())
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health/**", "/actuator/prometheus", "/actuator/info", "/api/v1/public/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationEntryPoint customAuthenticationEntryPoint() {
        return (request, response, authException) -> {
            meterRegistry.counter("devsphere_auth_authorization_denied_total", "reason", "unauthenticated").increment();
            log.warn("Authentication failed for URI: {} {}, reason: unauthenticated", request.getMethod(), request.getRequestURI());

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            String json = """
                    {
                      "status": 401,
                      "error": "UNAUTHORIZED",
                      "code": "UNAUTHORIZED",
                      "message": "Authentication is required"
                    }
                    """;
            response.getOutputStream().write(json.getBytes(StandardCharsets.UTF_8));
        };
    }

    @Bean
    public AccessDeniedHandler customAccessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            meterRegistry.counter("devsphere_auth_authorization_denied_total", "reason", "forbidden").increment();
            log.warn("Access denied for URI: {} {}, reason: forbidden", request.getMethod(), request.getRequestURI());

            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            String json = """
                    {
                      "status": 403,
                      "error": "FORBIDDEN",
                      "code": "FORBIDDEN",
                      "message": "You do not have permission to access this resource"
                    }
                    """;
            response.getOutputStream().write(json.getBytes(StandardCharsets.UTF_8));
        };
    }
}
