package org.telemetry.sensor;

import java.util.Random;

public class RpmSensor implements Sensor {
    private double targetRpm;
    private final double maxSafeRpm;
    private final Random random = new Random();

    public RpmSensor(double initialRpm, double maxSafeRpm) {
        this.targetRpm = initialRpm;
        this.maxSafeRpm = maxSafeRpm;
    }

    // The Autopilot uses this to "press the gas pedal"
    public void setTargetRpm(double targetRpm) {
        this.targetRpm = Math.min(targetRpm, maxSafeRpm);
    }

    @Override
    public String getName() { return "Engine RPM"; }

    @Override
    public String getUnit() { return "RPM"; }

    @Override
    public double readValue() {
        // Real engines have slight mechanical vibrations they don't hold perfectly still
        double noise = (random.nextDouble() * 50.0) - 25.0;
        return targetRpm + noise;
    }
}