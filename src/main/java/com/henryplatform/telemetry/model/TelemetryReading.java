package com.henryplatform.telemetry.model;

import com.henryplatform.telemetry.encryption.EncryptedFieldConverter;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "telemetry_readings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TelemetryReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vin", nullable = false)
    private String vin;

    // SEC: temperatura e degradação de óleo revelam padrão de uso do veículo (dado sensível do proprietário)
    //      criptografados em repouso para conformidade com LGPD art. 46
    @Convert(converter = EncryptedFieldConverter.class)
    @Column(name = "engine_temp_celsius")
    private String engineTempCelsius;

    @Convert(converter = EncryptedFieldConverter.class)
    @Column(name = "oil_life_percent")
    private String oilLifePercent;

    @Column(name = "rpm")
    private Integer rpm;

    @Column(name = "fuel_level_percent")
    private Integer fuelLevelPercent;

    @Column(name = "fault_codes")
    private String faultCodes;

    @Column(name = "health_score")
    private Integer healthScore;

    @Column(name = "processed", nullable = false)
    private Boolean processed;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @PrePersist
    public void prePersist() {
        this.recordedAt = LocalDateTime.now();
        this.processed = false;
    }
}
