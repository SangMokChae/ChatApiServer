package kr.co.dataric.chatapi.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.dataric.chatapi.config.sink.ChatSinkManager;
import kr.co.dataric.chatapi.kafka.producer.KafkaChatProducer;
import kr.co.dataric.chatapi.repository.room.CustomChatRoomRepository;
import kr.co.dataric.chatapi.service.ChatService;
import kr.co.dataric.chatapi.service.impl.ChatRoomLastReadService;
import kr.co.dataric.chatapi.service.impl.ChatRoomOnlineService;
import kr.co.dataric.common.dto.ChatMessageDTO;
import kr.co.dataric.common.dto.ReadReceiptEvent;
import kr.co.dataric.common.entity.ChatMessage;
import kr.co.dataric.common.jwt.provider.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketSession;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class HandlerSupport {
	
	private final ObjectMapper objectMapper;
	private final JwtProvider jwtProvider;
	
	public String extractRoomId(WebSocketSession session) {
		String[] parts = session.getHandshakeInfo().getUri().getPath().split("/");
		return parts.length > 0 ? parts[parts.length - 1] : null;
	}
	
	public String extractUserIdFromCookie(WebSocketSession session) {
		return session.getHandshakeInfo().getCookies().getFirst("accessToken") != null ?
			jwtProvider.extractUserId(session.getHandshakeInfo().getCookies().getFirst("accessToken").getValue()) : null;
	}
	
	public String toJson(ChatMessageDTO message) {
		try {
			return objectMapper.writeValueAsString(message);
		} catch (JsonProcessingException e) {
			log.error("❌ 메시지 직렬화 실패", e);
			return "{}";
		}
	}
	
	public ChatMessageDTO toDto(ChatMessage entity) {
		return ChatMessageDTO.builder()
			.id(entity.getMsgId())
			.roomId(entity.getRoomId())
			.sender(entity.getSender())
			.message(entity.getMessage())
			.timestamp(entity.getTimestamp())
			.build();
	}
	
	public void forwardReadEvent(String payload, String userId, String roomId) {
		// Kafka, Redis 등에 이벤트 전송
		ReadReceiptEvent event = ReadReceiptEvent.builder()
			.roomId(roomId)
			.userId(userId)
			.msgId(payload) // 또는 payload를 JSON 파싱 후 msgId 추출
			.timestamp(LocalDateTime.now())
			.build();
		
//		kafkaTemplate.send("chat.read.receipt", event);
		log.info("✅ Kafka 읽음 이벤트 전송: {}", event);
	}
	
	public void handleUserStatusChange(String status, String userId, String roomId) {
		// 예: online/offline 캐시 저장
		String redisKey = "user_status:" + roomId;
//		redisService.setHash(redisKey, userId, status);
		log.info("📌 유저 상태 변경 처리: roomId={}, userId={}, status={}", roomId, userId, status);
	}
	
}
