package org.telemetry.sensor;

public interface Sensor {
    String getName();
    String getUnit();
    double readValue(); // Every sensor must have a way to read data
}