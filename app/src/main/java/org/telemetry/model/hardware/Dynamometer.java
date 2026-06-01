package org.telemetry.model.hardware;

public abstract class Dynamometer {
    private final String id;
    private final double maxLoadCapacityKw;

    public Dynamometer(String id, double maxLoadCapacityKw) {
        this.id = id;
        this.maxLoadCapacityKw = maxLoadCapacityKw;
    }

    public String getId() { return id; }
    public double getMaxLoadCapacityKw() { return maxLoadCapacityKw; }

    public abstract String getHardwareType();
}