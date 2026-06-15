package org.telemetry.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Embedded;

@Entity
public class TestCell {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    private Environment environment;

    private double trackSurfaceFriction;

    // Mandatory JPA no-args constructor
    protected TestCell() {}

    public TestCell(Environment environment, double trackSurfaceFriction) {
        this.environment = environment;
        this.trackSurfaceFriction = trackSurfaceFriction;
    }

    public Long getId() { return id; }
    public Environment getEnvironment() { return environment; }
    public double getTrackSurfaceFriction() { return trackSurfaceFriction; }

    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }
    public void setTrackSurfaceFriction(double trackSurfaceFriction) {
        this.trackSurfaceFriction = trackSurfaceFriction;
    }
}