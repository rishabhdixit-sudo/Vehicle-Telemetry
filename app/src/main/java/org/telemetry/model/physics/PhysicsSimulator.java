package org.telemetry.model.physics;

import org.telemetry.model.VehicleRequest;
import java.util.HashMap;
import java.util.Map;

public class PhysicsSimulator {
    private final VehicleRequest vehicle;
    private final double tempC;
    private final double pressureHpa;
    private final double trackFriction;

    // Stateful Variables (These carry over tick-to-tick)
    private double currentVelocityMps = 0.0;
    private double currentRpm = 800.0; // Starts at Idle
    private double currentCoolantTemp;
    private int currentGear = 1;

    public PhysicsSimulator(VehicleRequest vehicle, double tempC, double pressureHpa, double trackFriction) {
        this.vehicle = vehicle;
        this.tempC = tempC;
        this.pressureHpa = pressureHpa;
        this.trackFriction = trackFriction;
        this.currentCoolantTemp = Math.max(tempC, 20.0); // Cold start ambient temp
    }

    // Runs every 0.5 seconds during the dyno loop
    public Map<String, Double> calculateNextTick(double throttlePct, double timeStepSeconds) {

        // 1. Environmental Thermodynamics (Calls your PhysicsEngine!)
        double airDensity = PhysicsEngine.calculateAirDensity(tempC, pressureHpa);
        double powerCorrection = PhysicsEngine.calculatePowerCorrection(airDensity);

        // 2. Powertrain Output
        double actualEngineTorque = throttlePct * vehicle.maxEngineTorqueNm() * powerCorrection;
        double gearRatio = vehicle.gearRatios().get(currentGear - 1);
        double wheelTorque = actualEngineTorque * gearRatio * vehicle.finalDriveRatio() * vehicle.transmissionEfficiency();

        // 3. Forces & Kinematics
        double tractiveForce = wheelTorque / vehicle.wheelRadiusMeters();
        double aeroDrag = PhysicsEngine.calculateAerodynamicDrag(airDensity, vehicle.aerodynamicDrag(), currentVelocityMps);
        double rollingResist = PhysicsEngine.calculateRollingResistance(vehicle.weightKg(), trackFriction);

        double netForce = tractiveForce - aeroDrag - rollingResist;

        // Simulate Engine Braking if AI lets off the throttle while moving
        if (throttlePct <= 0.05 && currentVelocityMps > 0) {
            netForce -= 1500;
        }

        // 4. Acceleration (F = MA)
        double acceleration = netForce / vehicle.weightKg();
        currentVelocityMps += acceleration * timeStepSeconds;
        if (currentVelocityMps < 0) currentVelocityMps = 0.0; // Cannot go backwards

        // 5. RPM Back-Calculation
        double wheelRpm = (currentVelocityMps / vehicle.wheelRadiusMeters()) * (60.0 / (2 * Math.PI));
        currentRpm = wheelRpm * gearRatio * vehicle.finalDriveRatio();

        // 6. Automatic Transmission Logic
        if (currentRpm > vehicle.maxSafeRpm() * 0.95 && currentGear < vehicle.gearRatios().size()) {
            currentGear++; // Upshift
            currentRpm = (currentVelocityMps / vehicle.wheelRadiusMeters()) * (60.0 / (2 * Math.PI)) * vehicle.gearRatios().get(currentGear - 1) * vehicle.finalDriveRatio();
        } else if (currentRpm < 1500 && currentGear > 1) {
            currentGear--; // Downshift
        }
        if (currentRpm < 800) currentRpm = 800 + (Math.random() * 50); // Keep alive idle

        // 7. Thermal & Fluid Simulation (Smooth Curves)
        double loadFactor = (throttlePct * currentRpm) / vehicle.maxSafeRpm();

        // Coolant integrates heat over time, subtracts radiator airflow cooling
        currentCoolantTemp += (loadFactor * 1.5) - (currentVelocityMps * 0.015);
        if (currentCoolantTemp < tempC) currentCoolantTemp = tempC;

        // EGT reacts aggressively to throttle and load
        double egt = tempC + 300 + (throttlePct * 450) + (currentRpm / vehicle.maxSafeRpm() * 150);

        // AFR drops (rich) under load to protect engine, hovers around 14.7 otherwise
        double afr = 14.7 - (throttlePct * 2.5) + (Math.random() * 0.1);

        // Oil Pressure scales linearly with mechanical RPM
        double oilPressure = 20.0 + (currentRpm / vehicle.maxSafeRpm() * 65.0) + (Math.random() * 1.5);

        // Turbo PSI follows throttle but requires exhaust flow (RPM) to spool
        double turboPsi = (throttlePct * 18.0) * Math.min(1.0, currentRpm / (vehicle.maxSafeRpm() * 0.6));

        // 8. Map to dynamic sensors
        Map<String, Double> metrics = new HashMap<>();
        metrics.put("RPM", currentRpm);

        if (vehicle.requestedSensors() != null) {
            for (String sensor : vehicle.requestedSensors()) {
                String s = sensor.toLowerCase();
                if (s.contains("coolant")) metrics.put(sensor, currentCoolantTemp);
                else if (s.contains("intake") || s.contains("air")) metrics.put(sensor, tempC + (throttlePct * 8) - (currentVelocityMps * 0.02));
                else if (s.contains("oil")) metrics.put(sensor, oilPressure);
                else if (s.contains("exhaust") || s.contains("egt")) metrics.put(sensor, egt);
                else if (s.contains("fuel_ratio") || s.contains("afr")) metrics.put(sensor, afr);
                else if (s.contains("throttle")) metrics.put(sensor, throttlePct * 100.0);
                else if (s.contains("timing") || s.contains("ignition")) metrics.put(sensor, 15.0 + (currentRpm / vehicle.maxSafeRpm() * 20.0) - (throttlePct * 6));
                else if (s.contains("fuel_pressure")) metrics.put(sensor, 58.0 - (throttlePct * 4) + (Math.random() * 0.5));
                else if (s.contains("turbo") || s.contains("psi")) metrics.put(sensor, turboPsi);
            }
        }
        return metrics;
    }
}