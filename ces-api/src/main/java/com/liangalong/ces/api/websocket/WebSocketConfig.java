package com.liangalong.ces.api.websocket;

import com.liangalong.ces.core.scheduler.TaskSchedulerEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final TaskSchedulerEngine engine;

    public WebSocketConfig(TaskSchedulerEngine engine) {
        this.engine = engine;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(dispatchWebSocketHandler(), "/ws/dispatch")
                .setAllowedOrigins("*");
    }

    @Bean
    public DispatchWebSocketHandler dispatchWebSocketHandler() {
        return new DispatchWebSocketHandler(engine);
    }
}
