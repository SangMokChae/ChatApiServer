package kr.co.dataric.chatapi.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.dataric.chatapi.config.sink.StatusSinkManager;
import kr.co.dataric.chatapi.kafka.producer.ReadEventProducer;
import kr.co.dataric.chatapi.service.impl.ChatRoomLastReadService;
import kr.co.dataric.common.dto.ReadReceiptEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReadStatusWebSocketHandler implements WebSocketHandler {
	
	private final ObjectMapper objectMapper;
	private final ReadEventProducer readEventProducer;
	private final StatusSinkManager statusSinkManager;
	private final ChatRoomLastReadService chatRoomLastReadService;
	private final HandlerSupport handlerSupport;
	
	@Override
	public Mono<Void> handle(WebSocketSession session) {
		String roomId = handlerSupport.extractRoomId(session);
		String userId = handlerSupport.extractUserIdFromCookie(session);
		
		if (roomId == null || userId == null) {
			log.warn("❌ WebSocket 연결 거부 - roomId 또는 userId 누락");
			return session.close();
		}
		
		// ✅ Sinks 생성 및 등록 (직접 생성 필요)
		Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();
		statusSinkManager.getOrCreate(roomId).add(sink);
		
		log.info("✅ ReadStatus WebSocket 연결 - roomId: {}, userId: {}", roomId, userId);
		
		Mono<Void> input = session.receive()
			.map(WebSocketMessage::getPayloadAsText)
			.flatMap(payload -> {
				try {
					JsonNode root = objectMapper.readTree(payload);
					List<Mono<Void>> actions = new ArrayList<>();
					
					// 1. 읽음 처리
					if (root.has("msgId") && root.has("participants")) {
						ReadReceiptEvent readEvent = objectMapper.treeToValue(root, ReadReceiptEvent.class);
						session.getAttributes().put("lastMessageId", readEvent.getMsgId());
						actions.add(readEventProducer.sendReadEvent(readEvent));
					}
					
					// 2. 상태 처리
					if (root.hasNonNull("status")) {
						String status = root.get("status").asText();
						String statusJson = objectMapper.writeValueAsString(Map.of(
							"type", "status",
							"userId", userId,
							"status", status,
							"roomId", roomId
						));
						statusSinkManager.get(roomId).forEach(s -> s.tryEmitNext(statusJson));
						log.info("📢 상태 전파: {}", statusJson);
					}
					
					return Mono.when(actions);
				} catch (Exception e) {
					log.error("❌ WebSocket 메시지 파싱 실패: {}", payload, e);
					return Mono.empty();
				}
			})
			.doFinally(signal -> {
				statusSinkManager.remove(roomId, sink);
				
				chatRoomLastReadService.syncAllLastReadFromRedisToMongo(roomId).subscribe();
				
				log.info("❎ WebSocket 연결 종료 - roomId: {}, userId: {}", roomId, userId);
			})
			.then();
		
		Flux<WebSocketMessage> output = sink.asFlux()
			.map(session::textMessage)
			.onErrorResume(e -> {
				log.warn("❗ 출력 스트림 오류: {}", e.toString());
				return Flux.empty();
			});
		
		return session.send(output).and(input);
	}
	
	public void broadcastUserStatus(String roomId, String userId, String status) {
		Set<Sinks.Many<String>> sinks = statusSinkManager.get(roomId);
		if (sinks == null || sinks.isEmpty()) {
			log.debug("⚠️ 상태 브로드캐스트 대상 없음 - roomId: {}", roomId);
			return;
		}
		
		try {
			String json = objectMapper.writeValueAsString(Map.of(
				"type", "status",
				"userId", userId,
				"status", status,
				"roomId", roomId
			));
			
			sinks.forEach(sink -> {
				Sinks.EmitResult result = sink.tryEmitNext(json);
				if (result.isFailure()) {
					log.warn("❗ 상태 메시지 전송 실패: {}, 이유: {}", json, result);
				}
			});
			log.info("📡 상태 브로드캐스트 완료 - roomId: {}, userId: {}, status: {}", roomId, userId, status);
		} catch (JsonProcessingException e) {
			log.error("❌ 상태 브로드캐스트 직렬화 실패", e);
		}
	}
	
	public void broadcastUserRead(String roomId, String userId, List<Map<String, String>> userReadList) {
		Set<Sinks.Many<String>> sinks = statusSinkManager.getOrCreate(roomId);
		if (sinks == null || sinks.isEmpty()) {
			log.debug("⚠️ 읽음 브로드캐스트 대상 없음 - roomId: {}", roomId);
			return;
		}
		
		try {
			String json = objectMapper.writeValueAsString(Map.of(
				"type", "readList",
				"roomId", roomId,
				"userId", userId,
				"readList", userReadList
			));
			
			sinks.forEach(sink -> {
				Sinks.EmitResult result = sink.tryEmitNext(json);
				if (result.isFailure()) {
					log.warn("❗ 읽음 메시지 전송 실패: {}, 이유: {}", json, result);
				}
			});
			log.info("📘 읽음 정보 브로드캐스트 완료 - roomId: {}, 대상: {}명", roomId, sinks.size());
		} catch (JsonProcessingException e) {
			log.error("❌ 읽음 브로드캐스트 직렬화 실패", e);
		}
	}
}
