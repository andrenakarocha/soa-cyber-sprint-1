package com.henryplatform.telemetry.controller;

import com.henryplatform.telemetry.audit.AuditLogger;
import com.henryplatform.telemetry.dto.AuthRequest;
import com.henryplatform.telemetry.dto.AuthResponse;
import com.henryplatform.telemetry.dto.RefreshRequest;
import com.henryplatform.telemetry.model.RefreshToken;
import com.henryplatform.telemetry.security.JwtTokenProvider;
import com.henryplatform.telemetry.security.UserRole;
import com.henryplatform.telemetry.service.RefreshTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "JWT authentication — token generation and renewal")
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final AuditLogger auditLogger;

    @Value("${security.jwt.expiration-ms}")
    private long expirationMs;

    @PostMapping("/token")
    @Operation(summary = "Generate access + refresh token",
               description = "Demo-only endpoint. In production, authentication happens via Ford's identity provider.")
    public ResponseEntity<?> generateToken(@Valid @RequestBody AuthRequest request) {
        // SEC: role parseada pelo enum — previne que valor arbitrário seja embutido no token
        UserRole role;
        try {
            role = UserRole.valueOf(request.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("status", 400, "message", "Invalid role"));
        }

        String accessToken  = jwtTokenProvider.generateToken(request.getSubject(), role);
        // SEC: refresh token emitido junto — permite renovação sem re-autenticação completa
        String refreshToken = refreshTokenService.createRefreshToken(request.getSubject(), role);

        auditLogger.log("AUTH_TOKEN_ISSUED", "User", request.getSubject());

        return ResponseEntity.ok(AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expirationMs / 1000)
                .build());
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renew access token using refresh token",
               description = "Issues a new access token and rotates the refresh token. Old refresh token is immediately invalidated.")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshRequest request) {
        // SEC: valida e rotaciona o refresh token — após este endpoint, o token antigo é inválido
        RefreshToken stored = refreshTokenService.validateAndRotate(request.getRefreshToken());
        UserRole role = UserRole.valueOf(stored.getRole());

        String newAccessToken  = jwtTokenProvider.generateToken(stored.getSubject(), role);
        String newRefreshToken = refreshTokenService.createRefreshToken(stored.getSubject(), role);

        auditLogger.log("TOKEN_REFRESHED", "User", stored.getSubject());

        return ResponseEntity.ok(AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(expirationMs / 1000)
                .build());
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke all refresh tokens for the authenticated user")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        // SEC: revogação em massa — garante que nenhum refresh token do usuário permanece ativo após logout
        RefreshToken stored = refreshTokenService.validateAndRotate(request.getRefreshToken());
        refreshTokenService.revokeAllForSubject(stored.getSubject());
        auditLogger.log("LOGOUT", "User", stored.getSubject());
        return ResponseEntity.noContent().build();
    }
}
