package com.henryplatform.telemetry.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data @Builder
public class HealthScoreResponse {
    private String vin;
    private Integer score;
    private String status;          // HEALTHY, WARNING, CRITICAL
    private String recommendation;  // mensagem que o HENRY exibirá ao cliente
    private LocalDateTime calculatedAt;
}
