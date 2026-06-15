package org.telemetry.model;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Transient;

@Embeddable
public class Environment {

    private double temperatureCelsius;
    private double atmosphericPressureHpa;

    @Transient
    private final double gasConstantR = 287.058;

    // Mandatory JPA no-args constructor
    protected Environment() {}

    public Environment(double temperatureCelsius, double atmosphericPressureHpa) {
        this.temperatureCelsius = temperatureCelsius;
        this.atmosphericPressureHpa = atmosphericPressureHpa;
    }

    public double getAirDensity() {
        double tempKelvin = temperatureCelsius + 273.15;
        double pressurePascals = atmosphericPressureHpa * 100.0;
        return pressurePascals / (gasConstantR * tempKelvin);
    }

    public double getPowerCorrectionFactor() {
        double standardDensity = 1.225;
        return getAirDensity() / standardDensity;
    }

    public static Environment standard() {
        return new Environment(20.0, 1013.25);
    }

    public double getTemperatureCelsius() { return temperatureCelsius; }
    public double getAtmosphericPressureHpa() { return atmosphericPressureHpa; }
}