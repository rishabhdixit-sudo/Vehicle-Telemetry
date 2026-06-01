package org.telemetry.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final LiveTelemetryHandler telemetryHandler;

    public WebSocketConfig(LiveTelemetryHandler telemetryHandler) {
        this.telemetryHandler = telemetryHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // We open the live pipe at ws://localhost:8080/ws/telemetry
        // .setAllowedOrigins("*") allows any local frontend tool to connect without security blocks
        registry.addHandler(telemetryHandler, "/ws/telemetry").setAllowedOrigins("*");
    }
}