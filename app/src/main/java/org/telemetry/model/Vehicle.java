package org.telemetry.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;
import org.telemetry.model.hardware.Transmission;
import org.telemetry.sensor.Sensor;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Vehicle {

    @Id
    private String vin; // The VIN is naturally unique

    private String model;
    private double maxSafeRpm;
    private double weightKg;
    private double maxEngineTorqueNm;
    private double aerodynamicDrag;
    private double wheelRadiusMeters;

    // Ignored by the database for now to keep things simple
    @Transient
    private Transmission transmission;

    @Transient
    private List<Sensor> customSensors;

    // Mandatory JPA no-args constructor
    protected Vehicle() {
        this.customSensors = new ArrayList<>();
    }

    public Vehicle(String vin, String model, double maxSafeRpm, double weightKg,
                   double maxEngineTorqueNm, double aerodynamicDrag,
                   Transmission transmission, double wheelRadiusMeters) {
        this.vin = vin;
        this.model = model;
        this.maxSafeRpm = maxSafeRpm;
        this.weightKg = weightKg;
        this.maxEngineTorqueNm = maxEngineTorqueNm;
        this.aerodynamicDrag = aerodynamicDrag;
        this.transmission = transmission;
        this.wheelRadiusMeters = wheelRadiusMeters;
        this.customSensors = new ArrayList<>();
    }

    public void addCustomSensor(Sensor sensor) { this.customSensors.add(sensor); }
    public List<Sensor> getCustomSensors() { return customSensors; }

    public String getVin() { return vin; }
    public String getModel() { return model; }
    public double getMaxSafeRpm() { return maxSafeRpm; }
    public double getWeightKg() { return weightKg; }
    public double getMaxEngineTorqueNm() { return maxEngineTorqueNm; }
    public double getAerodynamicDrag() { return aerodynamicDrag; }
    public Transmission getTransmission() { return transmission; }
    public double getWheelRadiusMeters() { return wheelRadiusMeters; }
}