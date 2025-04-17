package kr.co.dataric.chatapi.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.dataric.chatapi.config.sink.ChatSinkManager;
import kr.co.dataric.chatapi.kafka.producer.KafkaChatProducer;
import kr.co.dataric.chatapi.repository.room.CustomChatRoomRepository;
import kr.co.dataric.chatapi.service.ChatService;
import kr.co.dataric.chatapi.service.impl.ChatRoomLastReadService;
import kr.co.dataric.chatapi.service.impl.ChatRoomOnlineService;
import kr.co.dataric.common.dto.ChatMessageDTO;
import kr.co.dataric.common.dto.ChatRoomRedisDto;
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
	private final KafkaChatProducer kafkaChatProducer;
	private final CustomChatRoomRepository customChatRoomRepository;
	private final ChatRoomLastReadService chatRoomLastReadService;
	private final ChatRoomOnlineService chatRoomOnlineService;
	private final HandlerSupport handlerSupport;
	
	@Override
	public Mono<Void> handle(WebSocketSession session) {
		String roomId = handlerSupport.extractRoomId(session);
		String userId = handlerSupport.extractUserIdFromCookie(session);
		
		if (userId == null || roomId == null) {
			log.warn("❌ WebSocket 연결 거부 - userId 또는 roomId 누락");
			return session.close();
		}
		
		Sinks.Many<ChatMessageDTO> sink = chatSinkManager.register(roomId, userId);
		
		// 안전한 초기화 처리 + fallback
		session.getAttributes().put("lastReadMessageId", "INITIAL");
		chatRoomLastReadService.getLastReadMessage(roomId, userId)
			.defaultIfEmpty("INITIAL")
			.doOnNext(lastMsgId -> session.getAttributes().put("lastReadMessageId", lastMsgId))
			.subscribe();
		
		chatService.getMessagesByRoom(roomId, 0, 30)
			.collectList()
			.flatMapMany(messages -> {
				messages.sort(Comparator.comparing(ChatMessage::getTimestamp));
				return Flux.fromIterable(messages);
			})
			.map(handlerSupport::toDto)
			.doOnNext(sink::tryEmitNext)
			.subscribe();
		
		Mono<Void> input = session.receive()
			.map(WebSocketMessage::getPayloadAsText)
			.flatMap(payload -> {
				try {
					JsonNode json = objectMapper.readTree(payload);
					String msgId = Optional.ofNullable(json.get("msgId")).map(JsonNode::asText).orElse(UUID.randomUUID().toString());
					String message = Optional.ofNullable(json.get("message")).map(JsonNode::asText).orElse(null);
					if (message == null || message.isBlank()) return Mono.empty();
					
					LocalDateTime chatDate = LocalDateTime.now();
					ChatMessage msg = objectMapper.treeToValue(json, ChatMessage.class);
					msg.setMsgId(msgId);
					msg.setSender(userId);
					msg.setRoomId(roomId);
					msg.setMessage(message);
					msg.setTimestamp(chatDate);
					
					List<String> userIdsList = new ArrayList<>();
					JsonNode userIdArrayNode = json.get("inUserIds");
					if (userIdArrayNode != null && userIdArrayNode.isArray()) {
						for (JsonNode node : userIdArrayNode) {
							userIdsList.add(node.asText());
						}
					}
					
					if ("true".equals(Optional.ofNullable(json.get("isNewRoomMsg")).map(JsonNode::asText).orElse("false"))) {
						customChatRoomRepository.createNewChatRoom(roomId, userIdsList).subscribe();
					}
					
					session.getAttributes().put("lastMessageId", msgId);
					
					// WebSocket Sink 즉시 전송
					ChatMessageDTO dto = handlerSupport.toDto(msg);
					sink.tryEmitNext(dto);
					
					// Kafka는 후속 분산 처리용으로 전송 (메시지 전송 및 메시지 저장)
					kafkaChatProducer.sendMessage(msg);
					
					// Kafka 후속 분산 처리 - (ChatRoom Last 처리)
					ChatRoomRedisDto roomDto = new ChatRoomRedisDto();
					roomDto.setUserIds(userIdsList);
					roomDto.setRoomId(roomId);
					roomDto.setLastSender(userId);
					roomDto.setLastMessage(message);
					roomDto.setLastMessageTime(chatDate);
					
					kafkaChatProducer.updateChatRoom(roomDto);
					
					return Mono.empty();
				} catch (Exception e) {
					log.error("❌ WebSocket 수신 메시지 파싱 실패 - payload: {}", payload, e);
					return Mono.empty();
				}
			})
			// 해당 부분은 read로 변경할 것
			.doFinally(signalType -> {
				chatRoomOnlineService.removeUserFromOnline(roomId, userId).subscribe();
				String lastMessageId = Optional.ofNullable(session.getAttributes().get("lastMessageId"))
					.map(Object::toString)
					.orElse("INITIAL");
				chatRoomLastReadService.getLastReadMessage(roomId, userId)
					.defaultIfEmpty("INITIAL")
					.flatMap(existing -> !existing.equals(lastMessageId)
						? chatRoomLastReadService.updateLastReadMessage(roomId, userId, lastMessageId)
						: Mono.empty())
					.subscribe();
				chatSinkManager.unregister(roomId, userId, sink);
			})
			.then();
		
		Flux<WebSocketMessage> output = sink.asFlux()
			.map(handlerSupport::toJson)
			.map(session::textMessage)
			.onErrorResume(ex -> {
				log.warn("❌ WebSocket 출력 스트림 에러 발생: {}", ex.toString());
				return Flux.empty();
			});
		
		return session.send(output).and(input);
	}
}


