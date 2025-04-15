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
import java.util.*;

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
					String type = json.get("type").asText();
					
					// 메시지 전송 처리
					if ("chat".equals(type)) {
						String message = Optional.ofNullable(json.get("message")).map(JsonNode::asText).orElse(null);
						if (message == null || message.isBlank()) {
							log.warn("❌ 필수 필드 'message' 누락 또는 빈 값. payload: {}", payload);
							return Mono.empty();
						}
						
						String msgId = Optional.ofNullable(json.get("msgId")).map(JsonNode::asText).orElse(UUID.randomUUID().toString());
						LocalDateTime chatDate = LocalDateTime.now();
						
						ChatMessage msg = objectMapper.treeToValue(json, ChatMessage.class);
						msg.setMsgId(msgId);
						msg.setSender(userId);
						msg.setRoomId(roomId);
						msg.setMessage(message);
						msg.setTimestamp(chatDate);
						
						// inUserIds 파싱
						List<String> userIdsList = new ArrayList<>();
						JsonNode userIdArrayNode = json.get("inUserIds");
						if (userIdArrayNode != null && userIdArrayNode.isArray()) {
							for (JsonNode node : userIdArrayNode) {
								userIdsList.add(node.asText());
							}
						}
						
						// 새 방이면 생성
						if ("true".equals(Optional.ofNullable(json.get("isNewRoomMsg")).map(JsonNode::asText).orElse("false"))) {
							customChatRoomRepository.createNewChatRoom(roomId, userIdsList).subscribe();
						}
						
						// 저장 및 전파
						chatService.saveChatMessage(msg).subscribe();
						customChatRoomRepository.updateLastMessage(roomId, msg.getMessage(), chatDate).subscribe();
						kafkaChatProducer.sendMessage(msg);
						
					} else if ("read".equals(type)) {
						// 읽음 처리
						String msgId = json.get("msgId").asText();
						kafkaChatProducer.sendReadReceipt(msgId, userId, roomId);
					}
					
					return Mono.empty();
					
				} catch (Exception e) {
					log.error("❌ WebSocket 수신 메시지 파싱 실패 - payload: {}", payload, e);
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
