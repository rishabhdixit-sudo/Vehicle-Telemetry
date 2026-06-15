package org.telemetry.model.hardware;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Transient;
import org.telemetry.model.Vehicle;
import org.telemetry.model.TestCell;
import org.telemetry.model.physics.PhysicsEngine;

@Entity
public class VirtualChassisDyno extends Dynamometer {

    @OneToOne
    @JoinColumn(name = "vehicle_vin")
    private Vehicle vehicle;

    @ManyToOne
    @JoinColumn(name = "test_cell_id")
    private TestCell testCell;

    @Transient
    private final double dynoRollerInertiaKgM2 = 50.0;

    // Mandatory JPA no-args constructor
    protected VirtualChassisDyno() {}

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
        double airDensity = PhysicsEngine.calculateAirDensity(
                testCell.getEnvironment().getTemperatureCelsius(),
                testCell.getEnvironment().getAtmosphericPressureHpa()
        );
        double powerCorrection = PhysicsEngine.calculatePowerCorrection(airDensity);

        double maxRpm = vehicle.getMaxSafeRpm();
        double torqueMultiplier = (currentRpm < (maxRpm * 0.20)) ? 0.5 : (currentRpm < (maxRpm * 0.80)) ? 1.0 : 0.8;
        double currentEngineTorque = vehicle.getMaxEngineTorqueNm() * torqueMultiplier * throttlePercentage * powerCorrection;

        double gearRatio = vehicle.getTransmission().getCurrentGearRatio();
        double finalDrive = vehicle.getTransmission().getFinalDriveRatio();
        double wheelTorque = currentEngineTorque * gearRatio * finalDrive * vehicle.getTransmission().getEfficiency();

        double velocityMps = PhysicsEngine.calculateVelocityMetersPerSecond(currentRpm, gearRatio, finalDrive, vehicle.getWheelRadiusMeters());
        double aeroDragForce = PhysicsEngine.calculateAerodynamicDrag(airDensity, vehicle.getAerodynamicDrag(), velocityMps);
        double rollingResistanceForce = PhysicsEngine.calculateRollingResistance(vehicle.getWeightKg(), testCell.getTrackSurfaceFriction());

        double totalResistanceTorque = (aeroDragForce + rollingResistanceForce) * vehicle.getWheelRadiusMeters();
        double netTorque = wheelTorque - totalResistanceTorque;

        double totalInertia = dynoRollerInertiaKgM2 + 2.5 + (vehicle.getWeightKg() * 0.1);
        double angularAcceleration = netTorque / totalInertia;

        double rpmChange = (angularAcceleration * deltaTimeSeconds) * (60.0 / (2 * Math.PI));
        double newRpm = currentRpm + rpmChange;

        return Math.max(800.0, Math.min(newRpm, vehicle.getMaxSafeRpm()));
    }
}