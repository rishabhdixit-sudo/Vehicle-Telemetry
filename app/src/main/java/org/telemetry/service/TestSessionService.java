package org.telemetry.service;

import org.springframework.stereotype.Service;
import org.telemetry.model.*;
import org.telemetry.model.physics.PhysicsSimulator;
import org.telemetry.repository.TestReportRepository;
import org.telemetry.websocket.LiveTelemetryHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TestSessionService {

    private final Map<String, String> activeSessions = new ConcurrentHashMap<>();
    private final LiveTelemetryHandler broadcastHandler;
    //private final OllamaDiagnosticClient aiClient;
    private final GeminiDiagnosticClient aiClient;
    private final TestReportRepository reportRepository;

    public TestSessionService(LiveTelemetryHandler broadcastHandler, GeminiDiagnosticClient aiClient, TestReportRepository reportRepository) {
        this.broadcastHandler = broadcastHandler;
        this.aiClient = aiClient;
        this.reportRepository = reportRepository;
    }

    public void startRun(VehicleRequest request) {
        String vin = request.vin();
        if (activeSessions.containsKey(vin)) {
            System.out.println("Test already running for VIN: " + vin);
            return;
        }

        record ScenarioProfile(String name, double tempC, double friction) {}

        Map<String, ScenarioProfile> masterScenarios = Map.of(
                "Winter Cold Start", new ScenarioProfile("Winter Cold Start", -10.0, 0.4),
                "Desert Endurance", new ScenarioProfile("Desert Endurance", 45.0, 0.8),
                "Mountain Climb", new ScenarioProfile("Mountain Climb", 15.0, 0.6),
                "High Speed Wet", new ScenarioProfile("High Speed Wet", 10.0, 0.5),
                "Standard Factory Loop", new ScenarioProfile("Standard Factory Loop", 25.0, 0.8)
        );

        List<ScenarioProfile> playlist = new ArrayList<>();

        if ("COMPREHENSIVE".equalsIgnoreCase(request.testMode())) {
            playlist.addAll(masterScenarios.values());
        } else {
            if (request.customScenarios() != null && !request.customScenarios().isEmpty()) {
                for (String scenarioName : request.customScenarios()) {
                    if (masterScenarios.containsKey(scenarioName)) {
                        playlist.add(masterScenarios.get(scenarioName));
                    }
                }
            } else {
                playlist.add(masterScenarios.get("Standard Factory Loop"));
            }
        }

        new Thread(() -> {
            activeSessions.put(vin, "RUNNING");
            List<DataPoint> fullLog = new ArrayList<>();

            try {
                for (ScenarioProfile profile : playlist) {
                    System.out.println("ORCHESTRATOR QUEUING: " + profile.name());

                    String scenarioContext = String.format("%s at %.1f Celsius with %.1f grip", profile.name(), profile.tempC(), profile.friction());
                    DriveCycle cycle = aiClient.generatePreFlightScript(request, scenarioContext);

                    // INITIALIZE THE NEW REAL-WORLD PHYSICS ENGINE
                    PhysicsSimulator simulator = new PhysicsSimulator(request, profile.tempC(), request.atmosphericPressureHpa(), profile.friction());

                    // EXECUTE THE 60-SECOND LIVE LOOP
                    for (int tick = 0; tick < 120; tick++) {
                        // If user clicked 'Abort' in UI, this safely kills the thread
                        if (!activeSessions.containsKey(vin)) break;

                        double throttle = cycle.getTargetThrottle(tick);

                        // Advance physics by 0.5 seconds
                        Map<String, Double> metrics = simulator.calculateNextTick(throttle, 0.5);
                        metrics.put("CURRENT_SCENARIO", (double) profile.name().hashCode());

                        DataPoint dp = new DataPoint(System.currentTimeMillis(), metrics);
                        fullLog.add(dp);
                        broadcastHandler.broadcast(dp);

                        Thread.sleep(500);
                    }
                }

                activeSessions.remove(vin);

                String aiReport = aiClient.generateReport(fullLog, request.testMode());

                String rawJsonLog = new ObjectMapper().writeValueAsString(fullLog);
                reportRepository.save(new TestReport(request.vin(), request.model(), aiReport, rawJsonLog));
                System.out.println("Orchestration Complete! Saved to MySQL.");

                Map<String, Double> completeSignal = new java.util.HashMap<>();
                completeSignal.put("TEST_COMPLETE", 1.0);
                broadcastHandler.broadcast(new DataPoint(System.currentTimeMillis(), completeSignal));

            } catch (Exception e) {
                e.printStackTrace();
                activeSessions.remove(vin);
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    public void stopRun(String vin) {
        activeSessions.remove(vin);
    }
}