package org.telemetry.model;

import org.telemetry.model.hardware.Transmission; // NEW
import org.telemetry.sensor.Sensor;
import java.util.ArrayList;
import java.util.List;

public class Vehicle {
    private final String vin;
    private final String model;
    private final double maxSafeRpm;

    private final double weightKg;
    private final double maxEngineTorqueNm;
    private final double aerodynamicDrag;

    private final Transmission transmission;
    private final double wheelRadiusMeters;

    private final List<Sensor> customSensors;

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

    // Getters for the new components
    public Transmission getTransmission() { return transmission; }
    public double getWheelRadiusMeters() { return wheelRadiusMeters; }
}

