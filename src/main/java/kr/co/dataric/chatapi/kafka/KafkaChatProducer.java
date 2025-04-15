package kr.co.dataric.chatapi.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.dataric.common.dto.ReadReceiptEvent;
import kr.co.dataric.chatapi.repository.room.ChatRoomRepository;
import kr.co.dataric.common.dto.ChatRoomRedisDto;
import kr.co.dataric.common.entity.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaChatProducer {

	private final ChatRoomRepository chatRoomRepository;
	private final KafkaTemplate<String, Object> kafkaTemplate;
	private final ReactiveRedisTemplate<String, String> redisTemplate;
	private final ObjectMapper objectMapper;
	private static final String CHAT_TOPIC = "chat_messages";
	private static final String ROOM_UPDATE_TOPIC = "chat-room-update";
	
	public void sendMessage(ChatMessage message) {
		try {
			ProducerRecord<String, Object> chatRecord = new ProducerRecord<>(CHAT_TOPIC, message);
			chatRecord.headers().add("__TypeId__", "kr.co.dataric.common.dto.ChatMessageDTO".getBytes(StandardCharsets.UTF_8));
			kafkaTemplate.send(chatRecord)
				.whenComplete((result, ex) -> {
					if (ex != null) {
						log.error("Kafka 메시지 전송 실패", ex);
					} else {
						log.info("✅ Kafka 메시지 전송 성공: {}", result.getProducerRecord().value());
					}
				});
			
			sendChatRoomRedisUpdate(message); // ✅ 전파는 여기서 일괄 처리
			
		} catch (Exception e) {
			log.error("Kafka 메시지 전송 실패", e);
		}
	}
	
	public void sendReadReceipt(String msgId, String userId, String roomId) {
		try {
			ReadReceiptEvent event = ReadReceiptEvent.builder()
				.msgId(msgId)
				.userId(userId)
				.roomId(roomId)
				.timestamp(LocalDateTime.now()) // 또는 LocalDateTime.now()
				.build();
			
			ProducerRecord<String, Object> record = new ProducerRecord<>("chat.read", roomId, event);
			record.headers().add("__TypeId__", "kr.co.dataric.common.dto.ReadReceiptEvent".getBytes(StandardCharsets.UTF_8));
			
			kafkaTemplate.send(record);
			log.info("📩 Kafka 읽음 이벤트 전송 완료: {}", event);
		} catch (Exception e) {
			log.error("❌ Kafka 읽음 이벤트 전송 실패", e);
		}
	}
	
	public void sendChatRoomRedisUpdate(ChatMessage msg) {
		try {
			ChatRoomRedisDto dto = ChatRoomRedisDto.builder()
				.roomId(msg.getRoomId())
				.lastMessage(msg.getMessage())
				.lastMessageTime(msg.getTimestamp())
				.lastSender(msg.getSender()) // 반드시 msg.setSender 되어있어야함
				.build();
			
			String json = objectMapper.writeValueAsString(dto);
			
			Mono.fromFuture(() ->
					kafkaTemplate.send("chat.room.redis.update", msg.getRoomId(), json)
				).doOnSuccess(result -> log.info("✅ 전송 완료"))
				.doOnError(error -> log.error("❌ 전송 실패", error))
				.subscribe();
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}
}
