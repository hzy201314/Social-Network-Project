package com.social.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket 连接端点，前端通过这里连接
        registry.addEndpoint("/ws/chat")
                .setAllowedOriginPatterns("*")
                .withSockJS();  // 支持 SockJS 降级方案
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 消息代理前缀（订阅消息的地址）
        registry.enableSimpleBroker("/topic", "/queue", "/user");
        // 应用程序前缀（发送消息的地址）
        registry.setApplicationDestinationPrefixes("/app");
        // 用户消息前缀
        registry.setUserDestinationPrefix("/user");
    }
}