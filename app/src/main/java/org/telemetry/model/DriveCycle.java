package org.telemetry.model;

import java.util.List;

public class DriveCycle {
    private final String scenarioDescription;
    private final List<Double> throttleScript;

    public DriveCycle(String scenarioDescription, List<Double> throttleScript) {
        this.scenarioDescription = scenarioDescription;
        this.throttleScript = throttleScript;
    }

    public String getScenarioDescription() {
        return scenarioDescription;
    }

    // Fetches the exact throttle percentage the AI requested for this specific tick
    public double getTargetThrottle(int currentTick) {
        if (currentTick >= throttleScript.size()) {
            return 0.0; // Failsafe: Cut the throttle if the test goes out of bounds
        }
        return throttleScript.get(currentTick);
    }
}