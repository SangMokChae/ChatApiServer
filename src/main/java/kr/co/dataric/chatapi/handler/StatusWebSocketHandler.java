package kr.co.dataric.chatapi.handler;

import kr.co.dataric.chatapi.config.sink.StatusSinkManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatusWebSocketHandler implements WebSocketHandler {
	
	private final StatusSinkManager statusSinkManager;
	private final HandlerSupport handlerSupport;
	
	@Override
	public Mono<Void> handle(WebSocketSession session) {
		String roomId = handlerSupport.extractRoomId(session);
		String userId = handlerSupport.extractUserIdFromCookie(session);
		
		if (userId == null || roomId == null) {
			log.warn("❌ WebSocket 연결 거부 - userId 또는 roomId 누락");
			return session.close();
		}
		
		log.info("상태 WebSocket 연결됨 - roomId: {}, userId: {}", roomId, userId);
		
		Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();
		statusSinkManager.register(roomId, sink);
		
		Mono<Void> output = session.send(sink.asFlux().map(session::textMessage));
		
		Mono<Void> onClose = session.receive().then()
			.doFinally(signal -> {
				log.info("❎ 상태 WebSocket 종료 - roomId: {}, userId: {}", roomId, userId);
				statusSinkManager.remove(roomId, sink);
			});
		
		return Mono.zip(output, onClose).then(onClose);
	}
}
