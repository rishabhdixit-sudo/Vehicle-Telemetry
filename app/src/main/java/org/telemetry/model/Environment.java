package org.telemetry.model;

public class Environment {
    private final double temperatureCelsius;
    private final double atmosphericPressureHpa; // standard is 1013.25 hPa

    // Universal Gas Constant for dry air
    private final double gasConstantR = 287.058;

    public Environment(double temperatureCelsius, double atmosphericPressureHpa) {
        this.temperatureCelsius = temperatureCelsius;
        this.atmosphericPressureHpa = atmosphericPressureHpa;
    }


     // Calculates air density using the Ideal Gas Law: \rho = \frac{P}{R \cdot T}

    public double getAirDensity() {
        double tempKelvin = temperatureCelsius + 273.15;
        double pressurePascals = atmosphericPressureHpa * 100.0; // Convert hPa to Pascals
        return pressurePascals / (gasConstantR * tempKelvin);
    }

    /**
     * Engines make less power in thin/hot air.
     * Standard sea-level density is ~1.225 kg/m^3.
     */
    public double getPowerCorrectionFactor() {
        double standardDensity = 1.225;
        return getAirDensity() / standardDensity;
    }

    // Default garage environment (Standard Room Temperature)
    public static Environment standard() {
        return new Environment(20.0, 1013.25);
    }

    public double getTemperatureCelsius() {
        return temperatureCelsius;
    }

    public double getAtmosphericPressureHpa() {
        return atmosphericPressureHpa;
    }
}