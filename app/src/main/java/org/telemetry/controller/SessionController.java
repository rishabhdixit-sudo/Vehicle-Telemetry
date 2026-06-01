package org.telemetry.controller;

import org.springframework.web.bind.annotation.*;
import org.telemetry.service.TestSessionService;
import org.telemetry.model.VehicleRequest;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/test")
public class SessionController {

    private final TestSessionService sessionService;

    public SessionController(TestSessionService sessionService) {
        this.sessionService = sessionService;
    }

    // Now accepts our fully dynamic VehicleRequest
    @PostMapping("/start")
    public String startTest(@RequestBody VehicleRequest request) {
        sessionService.startRun(request);
        return "Command received: Assembled physics and started test for " + request.model() + " (VIN: " + request.vin() + ")";
    }

    @PostMapping("/stop/{vin}")
    public String stopTest(@PathVariable String vin) {
        sessionService.stopRun(vin);
        return "Command received: Stopped test for VIN " + vin;
    }
}