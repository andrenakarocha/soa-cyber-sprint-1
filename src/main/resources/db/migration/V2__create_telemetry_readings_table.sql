-- Leituras OBD-II recebidas dos veículos — base do health score calculado pelo HENRY
CREATE TABLE telemetry_readings (
    id                  BIGSERIAL   PRIMARY KEY,
    vin                 VARCHAR(17) NOT NULL REFERENCES vehicles(vin),
    -- SEC: campos de temperatura e óleo armazenados como TEXT cifrado (AES via EncryptedFieldConverter)
    --      dados de padrão de uso do veículo são considerados dados pessoais sensíveis pela LGPD
    engine_temp_celsius TEXT,
    oil_life_percent    TEXT,
    rpm                 INTEGER,
    fuel_level_percent  INTEGER,
    fault_codes         TEXT,
    health_score        INTEGER,
    processed           BOOLEAN     NOT NULL DEFAULT FALSE,
    recorded_at         TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_readings_vin ON telemetry_readings (vin);
CREATE INDEX idx_readings_recorded_at ON telemetry_readings (recorded_at DESC);
