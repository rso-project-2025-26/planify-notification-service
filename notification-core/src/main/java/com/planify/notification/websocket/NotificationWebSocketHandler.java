package com.planify.notification.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket. WebSocketSession;
import org. springframework.web.socket.handler. TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    // Hranimo aktiven session glede na userId
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String userId = (String) session.getAttributes().get("userId");
        if (userId != null) {
            sessions.put(userId, session);
            System.out.println("WebSocket connected for user: " + userId + " (Total connections: " + sessions.size() + ")");
        } else {
            System.err.println("WebSocket connection without userId");
            session.close(CloseStatus. POLICY_VIOLATION);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String userId = (String) session.getAttributes().get("userId");
        if (userId != null) {
            sessions.remove(userId);
            System.out.println("WebSocket disconnected for user: " + userId + " (Total connections: " + sessions.size() + ")");
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // Obravnavanje prihajajočih sporočil s client-a
        String userId = (String) session.getAttributes().get("userId");
        System.out.println("Received message from user " + userId + ": " + message.getPayload());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String userId = (String) session.getAttributes().get("userId");
        System.err.println("WebSocket error for user " + userId + ":  " + exception.getMessage());
    }

    /**
     * Pošlje obvestlo določenemu uporabniku
     */
    public void sendNotificationToUser(String userId, Object notification) {
        WebSocketSession session = sessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                String json = objectMapper.writeValueAsString(notification);
                session.sendMessage(new TextMessage(json));
                System.out.println("Notification sent to user:  " + userId);
            } catch (IOException e) {
                System.err.println("Error sending notification to user " + userId + ":  " + e.getMessage());
            }
        } else {
            System.out.println("User " + userId + " is not connected");
        }
    }

    /**
     * Broadcasta obvestilo vsem prijavljenim uporabnikom (trenutno se ne uporablja)
     */
    public void broadcastNotification(Object notification) {
        try {
            String json = objectMapper.writeValueAsString(notification);
            TextMessage message = new TextMessage(json);

            sessions.values().forEach(session -> {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(message);
                    } catch (IOException e) {
                        System.err. println("Error broadcasting to session:  " + e.getMessage());
                    }
                }
            });

            System.out.println("Notification broadcasted to " + sessions.size() + " users");
        } catch (IOException e) {
            System.err. println("Error creating broadcast message: " + e.getMessage());
        }
    }

    /**
     * Pridobi število prijavljenih uporabnikov (aktivnih session-ov)
     */
    public int getActiveConnectionCount() {
        return sessions. size();
    }

    /**
     * Preveri ali je uporabnik prijavljen (aktivno)
     */
    public boolean isUserConnected(String userId) {
        WebSocketSession session = sessions.get(userId);
        return session != null && session.isOpen();
    }
}