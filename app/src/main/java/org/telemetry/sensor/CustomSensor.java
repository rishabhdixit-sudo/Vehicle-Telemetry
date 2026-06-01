package org.telemetry.sensor;

import java.util.Random;

public class CustomSensor implements Sensor {
    private final String name;
    private final String unit;
    private final double baseValue;
    private final double rpmMultiplier;
    private final double noiseMargin;

    private final RpmSensor engineRpm;
    private final Random random = new Random();

    public CustomSensor(String name, String unit, double baseValue, double rpmMultiplier, double noiseMargin, RpmSensor engineRpm) {
        this.name = name;
        this.unit = unit;
        this.baseValue = baseValue;
        this.rpmMultiplier = rpmMultiplier;
        this.noiseMargin = noiseMargin;
        this.engineRpm = engineRpm;
    }

    @Override
    public String getName() { return name; }

    @Override
    public String getUnit() { return unit; }

    @Override
    public double readValue() {
        double currentRpm = engineRpm.readValue();
        double simulatedValue = baseValue + ((currentRpm / 1000.0) * rpmMultiplier);
        double noise = (random.nextDouble() * (noiseMargin * 2)) - noiseMargin;
        return simulatedValue + noise;
    }
}