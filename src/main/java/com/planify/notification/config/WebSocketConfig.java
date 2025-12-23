package com.planify.notification.config;

import com.planify.notification.websocket.NotificationWebSocketHandler;
import com.planify.notification.websocket.WebSocketAuthInterceptor;
import org.springframework.context. annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework. web.socket.config.annotation. WebSocketHandlerRegistry;
import org.springframework.web.socket. server.standard.ServletServerContainerFactoryBean;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final NotificationWebSocketHandler notificationHandler;
    private final WebSocketAuthInterceptor authInterceptor;

    public WebSocketConfig(NotificationWebSocketHandler notificationHandler,
                           WebSocketAuthInterceptor authInterceptor) {
        this.notificationHandler = notificationHandler;
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(notificationHandler, "/ws/notifications")
                .setAllowedOriginPatterns("*") // TODO Konfiguracija: nastavi na vrednost iz konfiguracije
                .addInterceptors(authInterceptor);
    }

    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(8192);
        container.setMaxBinaryMessageBufferSize(8192);
        return container;
    }
}