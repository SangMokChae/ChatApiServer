package kr.co.dataric.chatapi.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.dataric.chatapi.config.sink.ChatSinkManager;
import kr.co.dataric.chatapi.kafka.KafkaChatProducer;
import kr.co.dataric.chatapi.repository.room.CustomChatRoomRepository;
import kr.co.dataric.chatapi.service.ChatService;
import kr.co.dataric.common.entity.ChatMessage;
import kr.co.dataric.common.jwt.provider.JwtProvider;
import kr.co.dataric.common.redis.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler implements WebSocketHandler {
	
	private final ObjectMapper objectMapper;
	private final ChatSinkManager chatSinkManager;
	private final ChatService chatService;
	private final JwtProvider jwtProvider;
	private final KafkaChatProducer kafkaChatProducer;
	private final RedisService redisService;
	private final CustomChatRoomRepository customChatRoomRepository;
	
	@Override
	public Mono<Void> handle(WebSocketSession session) {
		String roomId = extractRoomId(session);
		String userId = extractUserIdFromCookie(session);
		
		if (userId == null || roomId == null) {
			log.warn("❌ WebSocket 연결 거부 - userId 또는 roomId 누락");
			return session.close();
		}
		
		Sinks.Many<ChatMessage> sink = chatSinkManager.getOrCreateSink(roomId, userId);
		
		chatService.getMessagesByRoom(roomId, 0, 30)
			.collectList()
			.flatMapMany(messages -> {
				messages.sort(Comparator.comparing(ChatMessage::getTimestamp));
				return Flux.fromIterable(messages);
			})
			.doOnNext(sink::tryEmitNext)
			.subscribe();
		
		Mono<Void> input = session.receive()
			.map(WebSocketMessage::getPayloadAsText)
			.flatMap(payload -> {
				try {
					JsonNode json = objectMapper.readTree(payload);
					LocalDateTime chatDate = LocalDateTime.now();
					
					ChatMessage msg = objectMapper.treeToValue(json, ChatMessage.class);
					msg.setMsgId(String.valueOf(UUID.randomUUID()));
					msg.setSender(userId);
					msg.setRoomId(roomId);
					msg.setTimestamp(chatDate);
					chatService.saveChatMessage(msg).subscribe();
					
					List<String> userIdsList = List.of(json.get("inUserIds").asText().split(","));
					
					// Kafka로 읽음 처리 이벤트 전송
					kafkaChatProducer.sendReadReceipt(msg.getMsgId(), userId, roomId);
					
					// 새로운 채팅방 생성 (주로 1:1 room) --> 새로 방만들거나, 단체방을 생성했을 경우 FrontEnd 단에서 false로 전송을 해줘야 한다.
					if(json.get("isNewRoomMsg").asText().equals("true")) {
						customChatRoomRepository.createNewChatRoom(roomId, userIdsList).subscribe();
					}
					
					kafkaChatProducer.sendMessage(msg);
					
					// mongodb에 채팅방 마지막 값 저장
					customChatRoomRepository.updateLastMessage(roomId, msg.getMessage(), chatDate).subscribe();
					
					chatSinkManager.emitToRoom(roomId, msg);
					return Mono.empty();
				} catch (Exception e) {
					log.error("WebSocket 수신 메시지 파싱 실패", e);
					return Mono.empty();
				}
			})
			.doFinally(signalType -> chatSinkManager.removeSubscriber(roomId, userId))
			.then();
		
		Flux<WebSocketMessage> output = sink.asFlux()
			.map(this::toJson)
			.map(session::textMessage)
			.onErrorResume(ex -> {
				log.warn("❌ WebSocket 출력 스트림 에러 발생: {}", ex.toString());
				return Flux.empty();
			});
		
		return session.send(output).and(input);
	}
	
	private String extractRoomId(WebSocketSession session) {
		String[] parts = session.getHandshakeInfo().getUri().getPath().split("/");
		return parts.length > 0 ? parts[parts.length - 1] : null;
	}
	
	private String extractUserIdFromCookie(WebSocketSession session) {
		return session.getHandshakeInfo().getCookies().getFirst("accessToken") != null ?
			jwtProvider.extractUserId(session.getHandshakeInfo().getCookies().getFirst("accessToken").getValue()) : null;
	}
	
	private String toJson(ChatMessage message) {
		try {
			return objectMapper.writeValueAsString(message);
		} catch (JsonProcessingException e) {
			log.error("❌ 메시지 직렬화 실패", e);
			return "{}";
		}
	}
}
