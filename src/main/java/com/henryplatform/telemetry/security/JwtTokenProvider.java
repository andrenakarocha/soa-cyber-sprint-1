package com.henryplatform.telemetry.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    // SEC: chave derivada do segredo configurado — nunca hardcoded no código-fonte
    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtTokenProvider(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.expiration-ms}") long expirationMs) {
        // SEC: HMAC-SHA256 requer chave de no mínimo 256 bits para garantir integridade do token
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(String subject, UserRole role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(subject)
                // SEC: role embutida no token para RBAC stateless — sem consulta ao banco a cada request
                .claim("role", role.name())
                .issuedAt(now)
                // SEC: expiração curta (1h) reduz o impacto de tokens vazados
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    public Claims validateAndExtract(String token) {
        // SEC: lança exceção específica para tokens expirados, inválidos ou adulterados
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            validateAndExtract(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
