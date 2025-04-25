package kr.co.dataric.chatapi.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.dataric.chatapi.config.sink.ReadSinkManager;
import kr.co.dataric.chatapi.config.sink.StatusSinkManager;
import kr.co.dataric.chatapi.kafka.producer.ReadEventProducer;
import kr.co.dataric.common.dto.ReadReceiptEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReadWebSocketHandler implements WebSocketHandler {
	
	private final HandlerSupport handlerSupport;
	private final ReadEventProducer readEventProducer;
	private final ReadSinkManager readSinkManager;
	private final ObjectMapper objectMapper;
	
	@Override
	public Mono<Void> handle(WebSocketSession session) {
		String roomId = handlerSupport.extractRoomId(session);
		String userId = handlerSupport.extractUserIdFromCookie(session);
	
		if (userId == null || roomId == null) {
			log.warn("Read WebSocket 연결 거부 - userId 또는 roomId 누락");
			return session.close();
		}
		
		log.info("Read WebSocket 연결됨 - roomId: {}, userId: {}", roomId, userId);
		
		return session.receive()
			.map(WebSocketMessage::getPayloadAsText)
			.flatMap(payload -> {
				try {
					ReadReceiptEvent event = objectMapper.readValue(payload, ReadReceiptEvent.class);
					
					// 필수 필드 확인
					if (event.getMsgId() == null || event.getUserId() == null || event.getRoomId() == null) {
						log.warn("ReadReceiptEvent 필드 누락: {}", event);
						return Mono.empty();
					}
					
					log.debug("읽음 이벤트 수신: {}", event);
					return readEventProducer.sendReadEvent(event);
				} catch (Exception e) {
					log.error("Read WebSocket 파싱 오류 - payload: {}", payload, e);
					return Mono.empty();
				}
			})
			.doFinally(signal -> log.info("Read WebSocket 연결 종류 - roomId: {}, userId: {}, reason: {}", roomId, userId, signal))
			.then();
	}
	
	public void broadcastUserRead(String roomId, String userId, List<Map<String, String>> readList) {
		Set<Sinks.Many<String>> sinks = readSinkManager.get(roomId);
		if (sinks != null) {
			for (Sinks.Many<String> sink : sinks) {
				String json = handlerSupport.toJson(Map.of(
					"roomId", roomId,
					"userId", userId,
					"readList", readList
				));
				
				sink.tryEmitNext(json);
			}
			
		}
	}
}
