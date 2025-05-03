package com.dms.apigateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import com.dms.apigateway.config.*;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class JwtValidationFilter extends AbstractGatewayFilterFactory<JwtValidationFilter.Config> {

    private final JwtProperties jwtProperties;

    public JwtValidationFilter(JwtProperties jwtProperties) {
        super(Config.class);
        this.jwtProperties = jwtProperties;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String authHeader = request.getHeaders().getFirst(jwtProperties.getHeaderName());

            if (authHeader == null || !authHeader.startsWith(jwtProperties.getTokenPrefix())) {
                return onError(exchange, "No authorization header", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.replace(jwtProperties.getTokenPrefix(), "");

            try {
                // Ensure the secret key is at least 64 bytes (512 bits) long
                if (jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8).length < 64) {
                    throw new IllegalArgumentException("Secret key must be at least 64 bytes for HMAC SHA-512");
                }
                
                SecretKey key = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(key)
                        .build()
                        .parseClaimsJws(token)
                        .getBody();

                // Add custom headers for downstream services
                ServerHttpRequest modifiedRequest = request.mutate()
                        .header("X-User-Id", claims.getSubject())
                        .header("X-User-Roles", claims.get("roles", List.class).toString())
                        .build();

                return chain.filter(exchange.mutate().request(modifiedRequest).build());
            } catch (Exception e) {
                return onError(exchange, "Invalid JWT token: " + e.getMessage(), HttpStatus.UNAUTHORIZED);
            }
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        exchange.getResponse().setStatusCode(httpStatus);
        return exchange.getResponse().setComplete();
    }

    public static class Config {
        // Configuration properties can be added here if needed
    }
}