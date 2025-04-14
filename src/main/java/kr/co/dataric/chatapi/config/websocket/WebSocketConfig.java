package kr.co.dataric.chatapi.config.websocket;

import kr.co.dataric.chatapi.handler.ChatWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class WebSocketConfig {
	
	@Bean
	public HandlerMapping webSocketMapping(ChatWebSocketHandler handler) {
		Map<String, WebSocketHandler> map = Map.of("/ws/chat/**", handler);
		
		SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
		mapping.setOrder(-1); // DispatcherHandler보다 먼저 실행되게
		mapping.setUrlMap(map);
		return mapping;
	}
	
}
