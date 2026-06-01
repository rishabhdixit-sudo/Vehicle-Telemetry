package org.telemetry.model;

import org.telemetry.sensor.Sensor;
import org.telemetry.sensor.RpmSensor;
import org.telemetry.service.Autopilot;
import org.telemetry.model.hardware.VirtualChassisDyno;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestSession {
    private final Vehicle vehicle;
    private final RpmSensor rpmSensor;
    private final VirtualChassisDyno dyno;

    private Autopilot autopilot;

    private final List<DataPoint> telemetryLog;
    private volatile boolean isRunning = true;

    // Engine RPM stays alive between different AI test scenarios
    private double currentRpm = 800.0;

    public TestSession(Vehicle vehicle, RpmSensor rpmSensor, VirtualChassisDyno dyno, Autopilot autopilot) {
        this.vehicle = vehicle;
        this.rpmSensor = rpmSensor;
        this.dyno = dyno;
        this.autopilot = autopilot;
        this.telemetryLog = new ArrayList<>();
    }

    // The hot-swap method for the AI playlist orchestrator
    public void setAutopilot(Autopilot newAutopilot) {
        this.autopilot = newAutopilot;
    }

    public void runTest(java.util.function.Consumer<DataPoint> onDataGenerated) throws InterruptedException {
        System.out.printf("--- Executing AI-Generated Drive Script for VIN: %s ---%n", vehicle.getVin());

        // We now loop for exactly 120 ticks (60 seconds per scenario)
        for (int i = 0; i < 120 && isRunning; i++) {

            // 1. Robot driver presses the pedal using the AI's exact JSON instructions
            double throttle = autopilot.determineThrottle(i);

            // 2. PHYSICS CALCULATOR
            currentRpm = dyno.calculateNextRpm(currentRpm, throttle, 0.5);

            // SHIFT LOGIC If we hit 90% of max RPM, grab the next gear
            if (currentRpm > (vehicle.getMaxSafeRpm() * 0.90)) {
                if (vehicle.getTransmission().shiftUp()) {
                    System.out.println("⚙️ SHIFTING UP to Gear " + vehicle.getTransmission().getCurrentGearNumber());
                    currentRpm = currentRpm * 0.65;
                }
            }

            rpmSensor.setTargetRpm(currentRpm);

            // 3. The sensor reads the final physical result
            double measuredRpm = rpmSensor.readValue();

            Map<String, Double> currentMetrics = new HashMap<>();
            currentMetrics.put("RPM", measuredRpm);

            for (Sensor sensor : vehicle.getCustomSensors()) {
                currentMetrics.put(sensor.getName(), sensor.readValue());
            }

            // Package the numbers into our DTO with the timestamp
            DataPoint point = new DataPoint(System.currentTimeMillis(), currentMetrics);
            telemetryLog.add(point);

            if (onDataGenerated != null) {
                onDataGenerated.accept(point);
            }

            Thread.sleep(500);
        }
        System.out.println("--- Scenario Complete ---");
    }

    public void stop() { this.isRunning = false; }
    public List<DataPoint> getTelemetryLog() { return telemetryLog; }

    // Used by the Orchestrator to give to the Chief Mechanic AI at the end
    public List<DataPoint> getFullLog() { return telemetryLog; }
}