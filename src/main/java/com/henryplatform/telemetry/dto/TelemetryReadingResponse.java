package com.henryplatform.telemetry.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data @Builder
public class TelemetryReadingResponse {
    private Long id;
    private String vin;
    private Double engineTempCelsius;
    private Integer oilLifePercent;
    private Integer rpm;
    private Integer fuelLevelPercent;
    private String faultCodes;
    private Integer healthScore;
    private Boolean processed;
    private LocalDateTime recordedAt;
}
