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
import java.util.*;

@Service
public class GeminiDiagnosticClient {


    private final String GEMINI_API_KEY = "api key";

    private final String geminiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=" + GEMINI_API_KEY;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    // GENERATES THE SCRIPT FOR THE AUTOPILOT
    public DriveCycle generatePreFlightScript(VehicleRequest request, String scenarioDetails) {
        String prompt = String.format(
                "Act as an automated dynamometer script generator. Test a %s weighing %.1f kg. " +
                        "Scenario: %s. " +
                        "Generate a 60-second test script (120 ticks). " +
                        "Output ONLY a raw JSON array of 120 decimal numbers between 0.0 and 1.0 representing throttle percentage. " +
                        "Do not include any markdown, text, or explanations. Start with [ and end with ].",
                request.model(), request.weightKg(), scenarioDetails
        );

        System.out.println(" Gemini AI is writing drive script for: " + scenarioDetails);

        try {
            String aiResponse = sendToGemini(prompt);

            // Clean up the response in case Gemini adds markdown code blocks
            String cleanJson = aiResponse.replaceAll("```json", "").replaceAll("```", "").trim();
            int startIndex = cleanJson.indexOf("[");
            int endIndex = cleanJson.lastIndexOf("]");

            if (startIndex != -1 && endIndex != -1) {
                cleanJson = cleanJson.substring(startIndex, endIndex + 1);
            }

            List<Double> script = objectMapper.readValue(cleanJson, new TypeReference<List<Double>>(){});
            while (script.size() < 120) script.add(0.0); // Failsafe pad

            return new DriveCycle(scenarioDetails, script.subList(0, 120));
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("⚠️ Gemini failed script generation. Using safe fallback.");
            return new DriveCycle("Fallback Script", Collections.nCopies(120, 0.85));
        }
    }

    // GENERATES THE POST-TEST REPORT
    public String generateReport(List<DataPoint> telemetryData, String testMode) {
        System.out.println(" Gemini AI Generating " + testMode + " diagnostic report...");

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
            return sendToGemini(promptBuilder.toString()).trim();
        } catch (Exception e) {
            e.printStackTrace();
            return "AI Analysis Failed. Error: " + e.getMessage();
        }
    }

    //  THE CORE HTTP BRIDGE TO GOOGLE'S SERVERS
    private String sendToGemini(String prompt) throws Exception {

        Map<String, Object> textNode = Map.of("text", prompt);
        Map<String, Object> partsNode = Map.of("parts", List.of(textNode));
        Map<String, Object> requestBody = Map.of("contents", List.of(partsNode));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(geminiUrl))
                .header("Content-Type", "application/json")
                .timeout(java.time.Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // Print the exact response from Google to the console!
        System.out.println("🔍 RAW GEMINI RESPONSE: " + response.body());

        JsonNode rootNode = objectMapper.readTree(response.body());

        // Catch API Errors so Java doesn't crash with a NullPointerException
        if (rootNode.has("error")) {
            throw new Exception("Google API Error: " + rootNode.path("error").path("message").asText());
        }

        if (!rootNode.has("candidates") || rootNode.path("candidates").isEmpty()) {
            throw new Exception("Gemini returned an empty response. Check safety filters or quota.");
        }

        return rootNode.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
    }
}