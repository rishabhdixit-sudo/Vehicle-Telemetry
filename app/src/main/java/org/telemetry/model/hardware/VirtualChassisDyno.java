package org.telemetry.model.hardware;

import org.telemetry.model.Vehicle;
import org.telemetry.model.TestCell;
import org.telemetry.model.physics.PhysicsEngine;

public class VirtualChassisDyno extends Dynamometer {
    private final Vehicle vehicle;
    private final TestCell testCell; // The Dyno now lives inside the TestCell

    private final double dynoRollerInertiaKgM2 = 50.0;

    public VirtualChassisDyno(String id, double maxLoadCapacityKw, Vehicle vehicle, TestCell testCell) {
        super(id, maxLoadCapacityKw);
        this.vehicle = vehicle;
        this.testCell = testCell;
    }

    @Override
    public String getHardwareType() {
        return "HORIBA 48-Inch Virtual Chassis Dynamometer (Physics Engine v3.0)";
    }

    public double calculateNextRpm(double currentRpm, double throttlePercentage, double deltaTimeSeconds) {

        // 1. Get Environmental Data from the standalone Physics Engine
        double airDensity = PhysicsEngine.calculateAirDensity(
                testCell.getEnvironment().getTemperatureCelsius(),
                testCell.getEnvironment().getAtmosphericPressureHpa()
        );
        double powerCorrection = PhysicsEngine.calculatePowerCorrection(airDensity);

        // 2. Engine Math
        double maxRpm = vehicle.getMaxSafeRpm();
        double torqueMultiplier = (currentRpm < (maxRpm * 0.20)) ? 0.5 : (currentRpm < (maxRpm * 0.80)) ? 1.0 : 0.8;
        double currentEngineTorque = vehicle.getMaxEngineTorqueNm() * torqueMultiplier * throttlePercentage * powerCorrection;

        // 3. Drivetrain Math
        double gearRatio = vehicle.getTransmission().getCurrentGearRatio();
        double finalDrive = vehicle.getTransmission().getFinalDriveRatio();
        double wheelTorque = currentEngineTorque * gearRatio * finalDrive * vehicle.getTransmission().getEfficiency();

        // 4. Kinematics & Resistance (Delegated entirely to PhysicsEngine)
        double velocityMps = PhysicsEngine.calculateVelocityMetersPerSecond(currentRpm, gearRatio, finalDrive, vehicle.getWheelRadiusMeters());

        double aeroDragForce = PhysicsEngine.calculateAerodynamicDrag(airDensity, vehicle.getAerodynamicDrag(), velocityMps);

        double rollingResistanceForce = PhysicsEngine.calculateRollingResistance(vehicle.getWeightKg(), testCell.getTrackSurfaceFriction());

        // 5. Apply Forces to the Axle
        double totalResistanceTorque = (aeroDragForce + rollingResistanceForce) * vehicle.getWheelRadiusMeters();
        double netTorque = wheelTorque - totalResistanceTorque;

        // 6. Calculate Final Acceleration
        double totalInertia = dynoRollerInertiaKgM2 + 2.5 + (vehicle.getWeightKg() * 0.1);
        double angularAcceleration = netTorque / totalInertia;

        double rpmChange = (angularAcceleration * deltaTimeSeconds) * (60.0 / (2 * Math.PI));
        double newRpm = currentRpm + rpmChange;

        // Keep RPM within safe limits safely
        return Math.max(800.0, Math.min(newRpm, vehicle.getMaxSafeRpm()));
    }
}
