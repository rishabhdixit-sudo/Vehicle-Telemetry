package org.telemetry.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "test_reports")
public class TestReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String vin;
    private String modelName;

    @Column(columnDefinition = "TEXT")
    private String aiDiagnosticReport;

    @Column(columnDefinition = "LONGTEXT")
    private String rawTelemetryJson;

    private LocalDateTime testDate;

    // Required by Hibernate
    public TestReport() {}

    public TestReport(String vin, String modelName, String aiDiagnosticReport, String rawTelemetryJson) {
        this.vin = vin;
        this.modelName = modelName;
        this.aiDiagnosticReport = aiDiagnosticReport;
        this.rawTelemetryJson = rawTelemetryJson;
        this.testDate = LocalDateTime.now(); // Automatically stamp the time!
    }

    public Long getId() { return id; }
    public String getVin() { return vin; }
    public String getModelName() { return modelName; }
    public String getAiDiagnosticReport() { return aiDiagnosticReport; }
    public String getRawTelemetryJson() { return rawTelemetryJson; }


    public LocalDateTime getTestDate() { return testDate; }
}