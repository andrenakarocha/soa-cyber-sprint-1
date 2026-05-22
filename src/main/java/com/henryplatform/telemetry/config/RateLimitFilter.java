package com.henryplatform.telemetry.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter implements Filter {

    // SEC: mapa por IP — cada origem tem seu próprio bucket, evitando que um cliente esgote o limite dos outros
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Value("${security.rate-limit.capacity}")
    private int capacity;

    @Value("${security.rate-limit.refill-tokens}")
    private int refillTokens;

    @Value("${security.rate-limit.refill-minutes}")
    private int refillMinutes;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        var httpRequest = (HttpServletRequest) request;
        var httpResponse = (HttpServletResponse) response;

        String ip = httpRequest.getRemoteAddr();
        Bucket bucket = buckets.computeIfAbsent(ip, this::newBucket);

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            // SEC: resposta 429 sem detalhes internos — não revela a lógica de throttling ao atacante
            httpResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            httpResponse.getWriter().write("{\"error\":\"Too many requests. Please slow down.\"}");
        }
    }

    private Bucket newBucket(String ip) {
        // SEC: token bucket com recarga gradual — mais resistente a burst attacks do que janela fixa
        Refill refill = Refill.greedy(refillTokens, Duration.ofMinutes(refillMinutes));
        Bandwidth limit = Bandwidth.classic(capacity, refill);
        return Bucket.builder().addLimit(limit).build();
    }
}
