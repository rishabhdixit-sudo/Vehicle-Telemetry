package org.telemetry.model.hardware;

public class Transmission {
    private final double[] gearRatios;
    private final double finalDriveRatio;
    private final double efficiency; //NEW: e.g., 0.85 means a 15% power loss
    private int currentGearIndex = 0;

    public Transmission(double[] gearRatios, double finalDriveRatio, double efficiency) {
        this.gearRatios = gearRatios;
        this.finalDriveRatio = finalDriveRatio;
        this.efficiency = efficiency;
    }

    public double getEfficiency() { return efficiency; }

    public double getFinalDriveRatio() {
        return finalDriveRatio;
    }

    public double getCurrentGearRatio() {
        return gearRatios[currentGearIndex];
    }

    public int getCurrentGearNumber() {
        return currentGearIndex + 1; // 1-indexed for the UI
    }

    public boolean shiftUp() {
        if (currentGearIndex < gearRatios.length - 1) {
            currentGearIndex++;
            return true; // Shift successful
        }
        return false; // Already in top gear
    }

    public boolean shiftDown() {
        if (currentGearIndex > 0) {
            currentGearIndex--;
            return true;
        }
        return false;
    }
}