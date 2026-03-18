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

import java.util.Set;

/**
 * Фильтр аутентификации по API-ключу.
 * Защищает критичные эндпоинты от неавторизованного доступа:
 * - POST /api/ingest (приём данных от коллекторов)
 * - PUT /api/scoring/weights (изменение весов скоринга)
 * - POST/PUT/DELETE /api/sources (управление источниками)
 *
 * Эндпоинты, безопасные для frontend (не принимают внешние данные):
 * - POST /api/scoring/run (пересчёт на основе имеющихся данных)
 * - Все GET-запросы
 */
@Component
@Order(1)
public class ApiKeyAuthFilter implements WebFilter {

    private static final String API_KEY_HEADER = "X-API-Key";

    /** Пути, требующие API-ключ для write-операций (POST/PUT/DELETE). */
    private static final Set<String> PROTECTED_PATHS = Set.of(
            "/api/ingest",
            "/api/sources",
            "/api/scoring/weights"
    );

    @Value("${security.api-key}")
    private String apiKey;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        HttpMethod method = exchange.getRequest().getMethod();
        String path = exchange.getRequest().getURI().getPath();

        // GET и OPTIONS всегда доступны (read-only)
        if (method == HttpMethod.GET || method == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }

        // Проверяем, требует ли path API-ключ
        if (requiresApiKey(path)) {
            String providedKey = exchange.getRequest().getHeaders().getFirst(API_KEY_HEADER);
            if (providedKey == null || !providedKey.equals(apiKey)) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
        }

        return chain.filter(exchange);
    }

    private boolean requiresApiKey(String path) {
        return PROTECTED_PATHS.stream().anyMatch(path::startsWith);
    }
}
