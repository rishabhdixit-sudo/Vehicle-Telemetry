package org.telemetry.service;

import org.telemetry.model.DriveCycle;

public class Autopilot {
    private final DriveCycle driveCycle;

    public Autopilot(DriveCycle driveCycle) {
        this.driveCycle = driveCycle;
    }

    public double determineThrottle(int currentTick) {
        return driveCycle.getTargetThrottle(currentTick);
    }
}