package com.planify.notification.websocket;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype. Component;
import org.springframework. web.socket.WebSocketHandler;
import org.springframework.web. socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Component
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) throws Exception {

        if (request instanceof ServletServerHttpRequest) {
            String query = request.getURI().getQuery();

            if (query != null) {
                Map<String, String> queryParams = UriComponentsBuilder. fromUriString("?" + query)
                        . build()
                        .getQueryParams()
                        .toSingleValueMap();

                String token = queryParams.get("token");
                String userId = queryParams.get("userId");

                if (token != null && userId != null) {
                    // TODO: Validate JWT token here using your JWT service

                    attributes.put("userId", userId);
                    attributes.put("token", token);

                    System.out.println("WebSocket handshake successful for user: " + userId);
                    return true;
                }
            }

            System.err.println("WebSocket handshake failed:  Missing token or userId");
        }

        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        if (exception != null) {
            System.err.println("WebSocket handshake error: " + exception.getMessage());
        }
    }
}