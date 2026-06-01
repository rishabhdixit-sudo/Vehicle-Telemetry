package org.telemetry.model.physics;

public class PhysicsEngine {

    // Standard Constants
    private static final double GRAVITY = 9.81;
    private static final double STANDARD_AIR_DENSITY = 1.225;
    private static final double GAS_CONSTANT_R = 287.058;

    // Thermodynamics
    public static double calculateAirDensity(double tempCelsius, double pressureHpa) {
        double tempKelvin = tempCelsius + 273.15;
        double pressurePascals = pressureHpa * 100.0;
        return pressurePascals / (GAS_CONSTANT_R * tempKelvin);
    }

    public static double calculatePowerCorrection(double currentAirDensity) {
        return currentAirDensity / STANDARD_AIR_DENSITY;
    }

    // Kinematics & Resistance
    public static double calculateVelocityMetersPerSecond(double engineRpm, double gearRatio, double finalDrive, double wheelRadius) {
        double wheelRpm = engineRpm / (gearRatio * finalDrive);
        return (wheelRpm * 2 * Math.PI / 60.0) * wheelRadius;
    }

    public static double calculateAerodynamicDrag(double airDensity, double dragCoefficientArea, double velocityMps) {
        return 0.5 * airDensity * dragCoefficientArea * Math.pow(velocityMps, 2);
    }

    public static double calculateRollingResistance(double weightKg, double surfaceFriction) {
        return weightKg * GRAVITY * surfaceFriction;
    }
}