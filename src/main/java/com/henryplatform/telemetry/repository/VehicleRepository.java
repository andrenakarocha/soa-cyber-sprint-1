package com.henryplatform.telemetry.repository;

import com.henryplatform.telemetry.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, String> {

    // SEC: consulta por ownerId garante isolamento de dados entre proprietários — cliente A não acessa dados do cliente B
    Optional<Vehicle> findByVinAndOwnerId(String vin, String ownerId);
}
