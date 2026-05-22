package com.henryplatform.telemetry.security;

import com.henryplatform.telemetry.audit.AuditLogger;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

// SEC: filtro de verificação de integridade de payload — previne adulteração de dados em trânsito
//      clientes devem incluir o header X-Payload-Signature com HMAC-SHA256 do body
@Component
@RequiredArgsConstructor
public class PayloadSignatureFilter implements Filter {

    // SEC: apenas métodos com corpo precisam de assinatura — GET/DELETE não têm payload
    private static final Set<String> SIGNED_METHODS = Set.of(
        HttpMethod.POST.name(), HttpMethod.PATCH.name(), HttpMethod.PUT.name()
    );

    private final AuditLogger auditLogger;

    @Value("${security.payload-signature.secret}")
    private String secret;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        var httpRequest = (HttpServletRequest) request;
        var httpResponse = (HttpServletResponse) response;

        if (requiresSignature(httpRequest)) {
            // SEC: leitura do body via CachedBodyHttpServletRequest para não consumir o stream
            var cached = new CachedBodyHttpServletRequest(httpRequest);
            String body = new String(cached.getCachedBody(), StandardCharsets.UTF_8);
            String signature = httpRequest.getHeader("X-Payload-Signature");

            if (signature == null || signature.isBlank()) {
                auditLogger.logFailure("MISSING_SIGNATURE", "PayloadFilter", "Header X-Payload-Signature absent");
                httpResponse.setStatus(HttpStatus.BAD_REQUEST.value());
                httpResponse.setContentType("application/json");
                httpResponse.getWriter().write("{\"status\":400,\"message\":\"Missing X-Payload-Signature header\"}");
                return;
            }

            if (!HmacUtils.verify(body, secret, signature)) {
                // SEC: log de falha de integridade — pode indicar ataque man-in-the-middle ou replay
                auditLogger.logFailure("INVALID_SIGNATURE", "PayloadFilter", "Payload signature mismatch");
                httpResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
                httpResponse.setContentType("application/json");
                httpResponse.getWriter().write("{\"status\":401,\"message\":\"Payload signature verification failed\"}");
                return;
            }

            chain.doFilter(cached, response);
        } else {
            chain.doFilter(request, response);
        }
    }

    private boolean requiresSignature(HttpServletRequest request) {
        // SEC: apenas endpoints da API de telemetria (com body) exigem assinatura
        return SIGNED_METHODS.contains(request.getMethod())
            && request.getRequestURI().startsWith("/api/v1/vehicles");
    }
}
