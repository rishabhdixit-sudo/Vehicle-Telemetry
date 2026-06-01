package org.telemetry.model;

import java.util.Map;

// By using a Map, this single object can hold 2 parameters for a bicycle
// or 200 parameters for a Boeing 747
public record DataPoint(long timestamp, Map<String, Double> metrics) {
}