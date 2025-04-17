package kr.co.dataric.chatapi.config.websocket;

import kr.co.dataric.chatapi.handler.ChatWebSocketHandler;
import kr.co.dataric.chatapi.handler.NotifyWebSocketHandler;
import kr.co.dataric.chatapi.handler.ReadWebSocketHandler;
import kr.co.dataric.chatapi.handler.StatusWebSocketHandler;
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
	
	private final ChatWebSocketHandler chatWebSocketHandler;
	private final ReadWebSocketHandler readWebSocketHandler;
	private final NotifyWebSocketHandler notifyWebSocketHandler;
	private final StatusWebSocketHandler statusWebSocketHandler;
	
	@Bean
	public HandlerMapping webSocketMapping() {
		Map<String, WebSocketHandler> map = new HashMap<>(Map.of()); // chatSocket
		map.put("/ws/chat/**", chatWebSocketHandler);
		map.put("/ws/read/**", readWebSocketHandler);
		map.put("/ws/notify/**", notifyWebSocketHandler);
		map.put("/ws/status/**", statusWebSocketHandler);
		
		SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
		mapping.setOrder(-1); // DispatcherHandler보다 먼저 실행되게
		mapping.setUrlMap(map);
		return mapping;
	}
	
}
