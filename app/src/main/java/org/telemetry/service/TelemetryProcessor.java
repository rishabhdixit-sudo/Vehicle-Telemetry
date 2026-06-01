package org.telemetry.service;

import org.telemetry.model.DataPoint;
import java.util.List;

public class TelemetryProcessor {
    // The maximum allowed temperature jump between consecutive readings
    private final double maxTempSpike = 2.0;

    // The specific key we are looking for in the dynamic map
    private final String targetMetric = "Temp_C";

    public void analyzeStream(List<DataPoint> telemetryLog) {
        System.out.println("\n--- Starting Data Analysis ---");

        // Edge case: If we don't have enough data, skip analysis
        if (telemetryLog == null || telemetryLog.size() < 2) {
            System.out.println("Not enough data to analyze.");
            return;
        }

        // Two-pointer logic setup
        int left = 0;

        // The right pointer moves ahead through the array
        for (int right = 1; right < telemetryLog.size(); right++) {

            DataPoint olderReading = telemetryLog.get(left);
            DataPoint newerReading = telemetryLog.get(right);

            // Safely extract the temperature from our dynamic Map
            Double oldTemp = olderReading.metrics().get(targetMetric);
            Double newTemp = newerReading.metrics().get(targetMetric);

            // Make sure the metric actually exists for this specific vehicle
            if (oldTemp != null && newTemp != null) {
                double tempDifference = newTemp - oldTemp;

                // Check if the spike exceeds our safety threshold
                if (tempDifference > maxTempSpike) {
                    System.out.printf("⚠️ ANOMALY DETECTED: Sudden temp spike of %.2f°C at reading [%d] (Time: %d)%n",
                            tempDifference, right, newerReading.timestamp());
                }
            }

            // Move the left pointer forward to keep evaluating the next pair
            left++;
        }

        System.out.println("--- Analysis Complete ---");
    }
}