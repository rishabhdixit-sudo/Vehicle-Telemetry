package org.telemetry.service;

import org.springframework.stereotype.Service;
import org.telemetry.model.*;
import org.telemetry.model.hardware.Transmission;
import org.telemetry.model.hardware.VirtualChassisDyno;
import org.telemetry.repository.TestReportRepository;
import org.telemetry.sensor.CustomSensor;
import org.telemetry.sensor.RpmSensor;
import org.telemetry.websocket.LiveTelemetryHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TestSessionService {

    private final Map<String, TestSession> activeSessions = new ConcurrentHashMap<>();
    private final LiveTelemetryHandler broadcastHandler;
    private final OllamaDiagnosticClient aiClient;
    private final TestReportRepository reportRepository;

    public TestSessionService(LiveTelemetryHandler broadcastHandler, OllamaDiagnosticClient aiClient, TestReportRepository reportRepository) {
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

        // 1. Build Hardware
        // Convert the List<Double> from the UI into a primitive array for the Transmission
        double[] gearArray = request.gearRatios().stream().mapToDouble(Double::doubleValue).toArray();
        Transmission trans = new Transmission(gearArray, request.finalDriveRatio(), request.transmissionEfficiency());

        Vehicle vehicle = new Vehicle(
                request.vin(), request.model(), request.maxSafeRpm(),
                request.weightKg(), request.maxEngineTorqueNm(),
                request.aerodynamicDrag(), trans, request.wheelRadiusMeters()
        );

        RpmSensor rpm = new RpmSensor(800.0, vehicle.getMaxSafeRpm());

        if (request.requestedSensors() != null) {
            for (String sensorName : request.requestedSensors()) {
                vehicle.addCustomSensor(new CustomSensor(sensorName, "Units", 100.0, 10.0, 2.0, rpm));
            }
        }

        // 2. Initial Test Cell Setup
        TestCell testRoom = new TestCell(new Environment(request.temperatureCelsius(), request.atmosphericPressureHpa()), 0.8);
        VirtualChassisDyno dyno = new VirtualChassisDyno("HORIBA-48", 500.0, vehicle, testRoom);

        // 3. Define the Master Dictionary of Physics Profiles
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
            // Run everything
            playlist.addAll(masterScenarios.values());
        } else {
            // CUSTOM mode: Look at what the user checked in the UI
            if (request.customScenarios() != null && !request.customScenarios().isEmpty()) {
                for (String scenarioName : request.customScenarios()) {
                    if (masterScenarios.containsKey(scenarioName)) {
                        playlist.add(masterScenarios.get(scenarioName));
                    }
                }
            } else {
                // Failsafe
                playlist.add(masterScenarios.get("Standard Factory Loop"));
            }
        }

        // 4. Spin up the Background Thread
        new Thread(() -> {
            try {
                // Initialize an empty session container
                TestSession session = new TestSession(vehicle, rpm, dyno, null);
                activeSessions.put(vin, session);

                // Run the playlist sequentially
                for (ScenarioProfile profile : playlist) {
                    System.out.println("ORCHESTRATOR QUEUING: " + profile.name());

                    // 1. HOT-SWAP THE DYNO PHYSICS
                    testRoom.setEnvironment(new Environment(profile.tempC(), request.atmosphericPressureHpa()));
                    testRoom.setTrackSurfaceFriction(profile.friction());

                    // 2. HAVE AI WRITE THE SCRIPT FOR THIS SPECIFIC WEATHER
                    String scenarioContext = String.format("%s at %.1f Celsius with %.1f grip", profile.name(), profile.tempC(), profile.friction());
                    DriveCycle cycle = aiClient.generatePreFlightScript(request, scenarioContext);

                    // 3. PUT THE AI IN THE DRIVER'S SEAT AND RUN
                    session.setAutopilot(new Autopilot(cycle));
                    session.runTest(dataPoint -> broadcastHandler.broadcast(dataPoint));
                }

                activeSessions.remove(vin);

                // Final diagnostic report
                String aiReport = aiClient.generateReport(session.getFullLog(), request.testMode());

                // Persist Data
                String rawJsonLog = new ObjectMapper().writeValueAsString(session.getFullLog());
                reportRepository.save(new TestReport(vehicle.getVin(), vehicle.getModel(), aiReport, rawJsonLog));
                System.out.println("Orchestration Complete! Saved to MySQL.");

                // Send a signal to React that the test is officially over
                Map<String, Double> completeSignal = new java.util.HashMap<>();
                completeSignal.put("TEST_COMPLETE", 1.0);
                broadcastHandler.broadcast(new DataPoint(System.currentTimeMillis(), completeSignal));

            } catch (Exception e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    public void stopRun(String vin) {
        TestSession session = activeSessions.get(vin);
        if (session != null) {
            session.stop();
            activeSessions.remove(vin);
        }
    }
}