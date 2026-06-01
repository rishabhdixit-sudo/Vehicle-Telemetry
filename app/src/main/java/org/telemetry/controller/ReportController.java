package org.telemetry.controller;

import org.springframework.web.bind.annotation.*;
import org.telemetry.model.TestReport;
import org.telemetry.repository.TestReportRepository;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "*") // Allows React UI to fetch the data safely
public class ReportController {
    private final TestReportRepository repository;

    public ReportController(TestReportRepository repository) {
        this.repository = repository;
    }

    // Fetches the most recently completed test for a specific vehicle
    @GetMapping("/latest/{vin}")
    public TestReport getLatestReport(@PathVariable String vin) {
        List<TestReport> reports = repository.findByVinOrderByTestDateDesc(vin);
        if (reports.isEmpty()) {
            return null;
        }
        return reports.get(0); // Return the newest one
    }
}