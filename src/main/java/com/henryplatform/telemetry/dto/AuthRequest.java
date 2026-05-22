package com.henryplatform.telemetry.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuthRequest {
    @NotBlank
    private String subject;

    // SEC: role validada no lado do servidor — cliente não pode se auto-promover a FORD_ADMIN
    @NotBlank
    private String role;
}
