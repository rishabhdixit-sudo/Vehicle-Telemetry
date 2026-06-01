
package org.telemetry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TelemetryApp {
    public static void main(String[] args) {
        SpringApplication.run(TelemetryApp.class, args);

        System.out.println("Spring Boot Web Server is running on Port 8080");
    }
}