package kr.co.dataric.chatapi.handler;

import kr.co.dataric.chatapi.config.sink.StatusSinkManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatusWebSocketHandler_back implements WebSocketHandler {
	
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
		
		// 상태 정보 전송
		Map<String, Object> statusPayload = Map.of(
			"type", "status",
			"userId", userId,
			"status", "online",
			"roomId", roomId
		);
		
		String statusJson = handlerSupport.toJson(statusPayload);
		sink.tryEmitNext(statusJson);
		
		Mono<Void> output = session.send(sink.asFlux().map(session::textMessage));
		
		Mono<Void> onClose = session.receive().then()
			.doFinally(signal -> {
				log.info("❎ 상태 WebSocket 종료 - roomId: {}, userId: {}", roomId, userId);
				statusSinkManager.remove(roomId, sink);
				
				String offlineJson = handlerSupport.toJson(Map.of(
					"type", "status",
					"userId", userId,
					"status", "offline",
					"roomId", roomId
				));
				// 나머지 사용자에게 broadcast
				Set<Sinks.Many<String>> sinks = statusSinkManager.get(roomId);
				if (sinks != null) {
					for (Sinks.Many<String> otherSink : sinks) {
						otherSink.tryEmitNext(offlineJson);
					}
				}
			});
		
		return Mono.zip(output, onClose).then(onClose);
	}
	
	public void broadcastUserStatus(String roomId, String userId, String status) {
		Set<Sinks.Many<String>> sinks = statusSinkManager.get(roomId);
		if (sinks != null) {
			for (Sinks.Many<String> sink : sinks) {
				String json = handlerSupport.toJson(Map.of(
					"type", "status",
					"userId", userId,
					"status", status
				));
				sink.tryEmitNext(json);
			}
		}
	}
}
