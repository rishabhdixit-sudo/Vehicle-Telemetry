package org.telemetry.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.telemetry.model.DataPoint;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

// @Component tells Spring to build this and keep it in memory
@Component
public class LiveTelemetryHandler extends TextWebSocketHandler {

    // LLD Attribute: activeConnections (Thread-safe list for multiple monitors)
    private final List<WebSocketSession> activeConnections = new CopyOnWriteArrayList<>();

    // Built-in tool to instantly convert our DataPoint record into a JSON string
    private final ObjectMapper objectMapper = new ObjectMapper();

    // LLD Method: onConnect() (Spring calls it afterConnectionEstablished)
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        activeConnections.add(session);
        System.out.println("Live monitor connected! Total watchers: " + activeConnections.size());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        activeConnections.remove(session);
        System.out.println("Monitor disconnected. Total watchers: " + activeConnections.size());
    }

    // LLD Method: broadcast()
    public void broadcast(DataPoint dataPoint) {
        try {
            // 1. Convert the DataPoint to JSON
            String payload = objectMapper.writeValueAsString(dataPoint);
            TextMessage message = new TextMessage(payload);

            // 2. Push it to every active browser screen
            for (WebSocketSession session : activeConnections) {
                if (session.isOpen()) {
                    session.sendMessage(message);
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to broadcast telemetry: " + e.getMessage());
        }
    }
}