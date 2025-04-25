package kr.co.dataric.chatapi.config.websocket;

import kr.co.dataric.chatapi.handler.ChatWebSocketHandler;
import kr.co.dataric.chatapi.handler.NotifyWebSocketHandler;
import kr.co.dataric.chatapi.handler.ReadStatusWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;

import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class WebSocketConfig {
	
	private final ChatWebSocketHandler chatWebSocketHandler;
	private final ReadStatusWebSocketHandler readStatusWebSocketHandler;
	private final NotifyWebSocketHandler notifyWebSocketHandler;
	
	@Bean
	public HandlerMapping webSocketMapping() {
		Map<String, WebSocketHandler> map = new HashMap<>(Map.of()); // chatSocket
		map.put("/ws/chat/**", chatWebSocketHandler);
		map.put("/ws/rs/**", readStatusWebSocketHandler); // read+status
		map.put("/ws/notify/**", notifyWebSocketHandler);
		
		SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
		mapping.setOrder(-1); // DispatcherHandler보다 먼저 실행되게
		mapping.setUrlMap(map);
		return mapping;
	}
	
}
