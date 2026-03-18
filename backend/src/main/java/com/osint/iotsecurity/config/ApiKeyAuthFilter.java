package com.osint.iotsecurity.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Фильтр аутентификации по API-ключу.
 * Защищает write-эндпоинты (POST/PUT/DELETE) от неавторизованного доступа.
 * GET-запросы остаются доступны для frontend (read-only).
 */
@Component
@Order(1)
public class ApiKeyAuthFilter implements WebFilter {

    private static final String API_KEY_HEADER = "X-API-Key";

    @Value("${security.api-key}")
    private String apiKey;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        HttpMethod method = exchange.getRequest().getMethod();
        String path = exchange.getRequest().getURI().getPath();

        // GET-запросы и SSE-стрим доступны без ключа (read-only для frontend)
        if (method == HttpMethod.GET || method == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }

        // Write-операции требуют API-ключ
        if (path.startsWith("/api/")) {
            String providedKey = exchange.getRequest().getHeaders().getFirst(API_KEY_HEADER);
            if (providedKey == null || !providedKey.equals(apiKey)) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
        }

        return chain.filter(exchange);
    }
}
