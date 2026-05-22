package com.henryplatform.telemetry.service;

import com.henryplatform.telemetry.dto.HealthScoreResponse;
import com.henryplatform.telemetry.model.TelemetryReading;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class HealthScoreService {

    public HealthScoreResponse calculate(String vin, TelemetryReading reading) {
        int score = 100;

        double temp = Double.parseDouble(reading.getEngineTempCelsius());
        int oil = Integer.parseInt(reading.getOilLifePercent());

        if (temp > 110) score -= 30;
        else if (temp > 95) score -= 15;

        if (oil < 10) score -= 35;
        else if (oil < 25) score -= 20;
        else if (oil < 40) score -= 10;

        if (reading.getFaultCodes() != null && !reading.getFaultCodes().isBlank()) score -= 20;

        score = Math.max(0, score);

        String status = score >= 70 ? "HEALTHY" : score >= 40 ? "WARNING" : "CRITICAL";

        String recommendation = switch (status) {
            case "HEALTHY" -> "Seu veículo está em ótimas condições. Continue com as manutenções preventivas.";
            case "WARNING" -> "HENRY detectou pontos de atenção. Recomendo uma revisão em breve.";
            case "CRITICAL" -> "HENRY identificou problemas críticos. Agende uma revisão com urgência.";
            default -> "";
        };

        return HealthScoreResponse.builder()
                .vin(vin)
                .score(score)
                .status(status)
                .recommendation(recommendation)
                .calculatedAt(LocalDateTime.now())
                .build();
    }
}
