package org.telemetry.model;

import java.util.List;

public record VehicleRequest(
        String vin,
        String model,
        double maxSafeRpm,
        double weightKg,
        double maxEngineTorqueNm,
        double aerodynamicDrag,
        List<Double> gearRatios,
        double finalDriveRatio,
        double transmissionEfficiency,
        double wheelRadiusMeters,
        double temperatureCelsius,
        double atmosphericPressureHpa,
        List<String> requestedSensors,


        String testMode, // "TRADITIONAL", "CUSTOM", or "COMPREHENSIVE"
        List<String> customScenarios // The playlist from the frontend checklist
) {}