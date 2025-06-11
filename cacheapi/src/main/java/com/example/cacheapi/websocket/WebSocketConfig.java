package com.example.cacheapi.websocket;
import org.springframework.context.annotation.Bean;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new CacheWebSocketHandler(), "/ws/cache")
                .setAllowedOrigins("*");
    }

    @Bean
    public CacheWebSocketHandler cacheWebSocketHandler() {
        return new CacheWebSocketHandler();
    }
}
