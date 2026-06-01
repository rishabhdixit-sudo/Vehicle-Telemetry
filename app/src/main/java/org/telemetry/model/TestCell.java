package org.telemetry.model;

public class TestCell {
    private Environment environment;
    private double trackSurfaceFriction;

    public TestCell(Environment environment, double trackSurfaceFriction) {
        this.environment = environment;
        this.trackSurfaceFriction = trackSurfaceFriction;
    }

    public Environment getEnvironment() { return environment; }
    public double getTrackSurfaceFriction() { return trackSurfaceFriction; }

    //The Hot-Swap setters for the Orchestrator
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    public void setTrackSurfaceFriction(double trackSurfaceFriction) {
        this.trackSurfaceFriction = trackSurfaceFriction;
    }
}