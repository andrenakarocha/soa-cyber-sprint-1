package com.henryplatform.telemetry.repository;

import com.henryplatform.telemetry.model.TelemetryReading;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TelemetryReadingRepository extends JpaRepository<TelemetryReading, Long> {

    Page<TelemetryReading> findByVin(String vin, Pageable pageable);

    Optional<TelemetryReading> findTopByVinOrderByRecordedAtDesc(String vin);
}
