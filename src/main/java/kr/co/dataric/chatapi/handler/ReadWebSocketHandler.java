package kr.co.dataric.chatapi.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.dataric.chatapi.kafka.producer.ReadEventProducer;
import kr.co.dataric.common.dto.ReadReceiptEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReadWebSocketHandler implements WebSocketHandler {
	
	private final HandlerSupport handlerSupport;
	private final ReadEventProducer readEventProducer;
	private final ObjectMapper objectMapper;
	
	@Override
	public Mono<Void> handle(WebSocketSession session) {
		String roomId = handlerSupport.extractRoomId(session);
		String userId = handlerSupport.extractUserIdFromCookie(session);
		
		if (userId == null || roomId == null) {
			log.warn("❌ WebSocket 연결 거부 - userId 또는 roomId 누락");
			return session.close();
		}
		
		log.info("📘 읽음 WebSocket 연결됨: roomId={}, userId={}", roomId, userId);
		
		Mono<Void> readHandler = session.receive()
			.map(WebSocketMessage::getPayloadAsText)
			.flatMap(json -> {
				try {
					ReadReceiptEvent event = objectMapper.readValue(json, ReadReceiptEvent.class);
					
					// 기본값 보정
					event.setRoomId(Optional.ofNullable(event.getRoomId()).orElse(roomId));
					event.setUserId(Optional.ofNullable(event.getUserId()).orElse(userId));
					
					// 🔥 필수: participants 비어있으면 무시 (readCount 계산 못함)
					if (event.getParticipants() == null || event.getParticipants().isEmpty()) {
						log.warn("❌ 읽음 이벤트에 participants 누락됨: {}", event);
						return Mono.empty();
					}
					
					return readEventProducer.sendReadEvent(event);
				} catch (Exception e) {
					log.error("읽음 이벤트 파싱 실패", e);
					return Mono.empty();
				}
			})
			.doOnComplete(() -> log.info("📘 읽음 WebSocket 종료: roomId={}, userId={}", roomId, userId))
			.then();
		
		return readHandler;
	}
}
