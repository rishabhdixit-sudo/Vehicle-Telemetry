package org.telemetry.model.hardware;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Dynamometer {

    @Id
    private String id;

    private double maxLoadCapacityKw;

    // Mandatory JPA no-args constructor
    protected Dynamometer() {}

    public Dynamometer(String id, double maxLoadCapacityKw) {
        this.id = id;
        this.maxLoadCapacityKw = maxLoadCapacityKw;
    }

    public String getId() { return id; }
    public double getMaxLoadCapacityKw() { return maxLoadCapacityKw; }

    public abstract String getHardwareType();
}