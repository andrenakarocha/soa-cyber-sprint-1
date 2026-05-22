package com.henryplatform.telemetry.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshRequest {
    // SEC: refresh token recebido como campo de body JSON — não em query param para evitar log em acesso
    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}
