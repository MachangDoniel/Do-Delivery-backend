package com.dodelivery.app.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP over WebSocket configuration.
 *
 * <p>Clients connect to: {@code ws://host/ws} (or via SockJS fallback at {@code /ws}).
 *
 * <h3>Subscribe to real-time events:</h3>
 * <ul>
 *   <li>{@code /topic/orders/new}          — new delivery request (riders)
 *   <li>{@code /topic/orders/{orderId}}    — order status updates (customer + rider)
 *   <li>{@code /topic/riders/{riderId}/location} — live GPS position (customer)
 * </ul>
 *
 * <h3>Send messages from client:</h3>
 * <p>Prefix {@code /app} routes to {@code @MessageMapping} handler methods.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // In-memory simple broker for topic subscriptions
        registry.enableSimpleBroker("/topic");
        // Prefix for messages sent from client to server
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                // TODO: restrict to known origins in production
                .setAllowedOriginPatterns("*")
                .withSockJS();  // SockJS fallback for browsers without native WebSocket
    }
}
