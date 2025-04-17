package kr.co.dataric.chatapi.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotifyWebSocketHandler implements WebSocketHandler {
	
	private final HandlerSupport handlerSupport;

		@Override
		public Mono<Void> handle(WebSocketSession session) {
			String roomId = handlerSupport.extractRoomId(session);
			String userId = handlerSupport.extractUserIdFromCookie(session);
			
			if (userId == null || roomId == null) {
				log.warn("❌ WebSocket 연결 거부 - userId 또는 roomId 누락");
				return session.close();
			}
			
			return null;
	}
}
