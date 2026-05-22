package com.henryplatform.telemetry.security;

import com.henryplatform.telemetry.audit.AuditLogger;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final AuditLogger auditLogger;

    // SEC: contador de falhas por IP — detecta padrões de brute force ou token stuffing
    private final Map<String, AtomicInteger> failureCountByIp = new ConcurrentHashMap<>();

    @Value("${security.suspicious-threshold:5}")
    private int suspiciousThreshold;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String token = extractToken(request);

        if (token != null) {
            if (jwtTokenProvider.isValid(token)) {
                Claims claims = jwtTokenProvider.validateAndExtract(token);
                String role = claims.get("role", String.class);

                // SEC: autoridade prefixada com ROLE_ para compatibilidade com Spring Security RBAC
                var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
                var auth = new UsernamePasswordAuthenticationToken(claims.getSubject(), null, authorities);

                // SEC: autenticação injetada no contexto da thread — isolada por request
                SecurityContextHolder.getContext().setAuthentication(auth);

                // SEC: reset do contador ao autenticar com sucesso — evita falsos positivos
                failureCountByIp.remove(request.getRemoteAddr());
            } else {
                recordFailure(request);
            }
        }

        chain.doFilter(request, response);
    }

    private void recordFailure(HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        int failures = failureCountByIp
                .computeIfAbsent(ip, k -> new AtomicInteger(0))
                .incrementAndGet();

        if (failures >= suspiciousThreshold) {
            // SEC: alerta de evento suspeito — múltiplas falhas de JWT do mesmo IP podem indicar ataque
            auditLogger.logFailure(
                "SUSPICIOUS_JWT_FAILURES",
                "JwtAuthFilter",
                String.format("IP %s failed JWT validation %d times", ip, failures)
            );
        }
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        // SEC: exige esquema Bearer explícito — rejeita tokens passados de outra forma
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
