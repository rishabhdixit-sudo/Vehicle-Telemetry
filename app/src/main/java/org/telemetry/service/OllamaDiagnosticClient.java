package org.telemetry.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.telemetry.model.DataPoint;
import org.telemetry.model.DriveCycle;
import org.telemetry.model.VehicleRequest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;

@Service
public class OllamaDiagnosticClient {

    private final String ollamaUrl = "http://localhost:11434/api/generate";
    private final String modelName = "qwen2.5-coder:3b";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    // METHOD 1: GENERATES THE SCRIPT (Returns a DriveCycle)
    public DriveCycle generatePreFlightScript(VehicleRequest request, String scenarioDetails) {
        String prompt = String.format(
                "Act as an automated dynamometer script generator. Test a %s weighing %.1f kg. " +
                        "Scenario: %s. " +
                        "Generate a 60-second test script (120 ticks). " +
                        "Output ONLY a raw JSON array of 120 decimal numbers between 0.0 and 1.0 representing throttle percentage. " +
                        "Do not include any markdown, text, or explanations. Start with [ and end with ].",
                request.model(), request.weightKg(), scenarioDetails
        );

        System.out.println(" AI is writing drive script for: " + scenarioDetails);

        try {
            String aiResponse = sendToOllama(prompt);
            String cleanJson = aiResponse.replaceAll("```json", "").replaceAll("```", "").trim();
            int startIndex = cleanJson.indexOf("[");
            int endIndex = cleanJson.lastIndexOf("]");

            if (startIndex != -1 && endIndex != -1) {
                cleanJson = cleanJson.substring(startIndex, endIndex + 1);
            }

            List<Double> script = objectMapper.readValue(cleanJson, new TypeReference<List<Double>>(){});
            while (script.size() < 120) script.add(0.0);

            return new DriveCycle(scenarioDetails, script.subList(0, 120));
        } catch (Exception e) {
            // THIS IS WHERE WE CATCH SCRIPT ERRORS
            e.printStackTrace(); // This prints the exact reason why it failed!
            System.err.println("AI failed script generation or timed out! Using safe fallback.");
            return new DriveCycle("Fallback Script", Collections.nCopies(120, 0.85));
        }
    }

    // METHOD 2: GENERATES THE POST-TEST REPORT (Returns a String)
    public String generateReport(List<DataPoint> telemetryData, String testMode) {
        System.out.println("Generating " + testMode + " diagnostic report...");

        StringBuilder promptBuilder = new StringBuilder();

        promptBuilder.append("You are a strict Senior Automotive Mechanic evaluating dynamometer telemetry. You are NOT a programmer.\n");
        promptBuilder.append("CRITICAL INSTRUCTIONS:\n1. DO NOT write any Python, Java, or computer code.\n2. DO NOT output mathematical formulas.\n");

        if ("TRADITIONAL".equalsIgnoreCase(testMode) || "CUSTOM".equalsIgnoreCase(testMode)) {
            promptBuilder.append("3. Provide a short, standard factory pass/fail report.\n");
        } else {
            promptBuilder.append("3. This is a COMPREHENSIVE multi-scenario test. Provide a massive, in-depth component-by-component analysis.\n");
            promptBuilder.append("4. Identify specific performance bottlenecks and provide highly technical engineering improvements.\n");
        }

        promptBuilder.append("5. YOU MUST strictly use Markdown format with headings (e.g., Status Overview, Key Observations, Recommendations).\n\n");
        promptBuilder.append("Here is the telemetry data:\n```text\n");

        for (DataPoint dp : telemetryData) {
            StringBuilder line = new StringBuilder();
            for (Map.Entry<String, Double> entry : dp.metrics().entrySet()) {
                line.append(entry.getKey()).append(": ").append(String.format("%.1f", entry.getValue())).append(" | ");
            }
            promptBuilder.append(line.toString()).append("\n");
        }
        promptBuilder.append("```\n");

        try {
            return sendToOllama(promptBuilder.toString()).trim();
        } catch (Exception e) {
            // THIS IS WHERE WE CATCH REPORT ERRORS
            e.printStackTrace();
            return "AI Analysis Failed. Error: " + e.getMessage(); // Returns a string!
        }
    }

    private String sendToOllama(String prompt) throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", modelName);
        requestBody.put("prompt", prompt);
        requestBody.put("stream", false);

        Map<String, Object> options = new HashMap<>();
        options.put("temperature", 0.7);
        options.put("num_ctx", 4096);
        requestBody.put("options", options);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ollamaUrl))
                .header("Content-Type", "application/json")
                .timeout(java.time.Duration.ofSeconds(120)) // ⏱️ 15 Second cutoff prevents UI freeze!
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return objectMapper.readTree(response.body()).get("response").asText();
    }
}