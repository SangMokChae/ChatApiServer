package kr.co.dataric.chatapi.kafka.producer;

import kr.co.dataric.chatapi.config.sink.ChatSinkManager;
import kr.co.dataric.common.dto.ChatMessageDTO;
import kr.co.dataric.common.dto.ChatRoomRedisDto;
import kr.co.dataric.common.entity.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaChatProducer {
	
	private final KafkaTemplate<String, Object> kafkaTemplate;
	private final ChatSinkManager chatSinkManager;
	private static final String CHAT_SEND_TOPIC = "chat.room.send";
	private static final String REDIS_UPDATE_TOPIC = "chat.room.redis.update";
	
	// 채팅 전송
	public void sendMessage(ChatMessage msg) {
		ChatMessageDTO dto = ChatMessageDTO.builder()
			.id(msg.getMsgId())
			.roomId(msg.getRoomId())
			.sender(msg.getSender())
			.message(msg.getMessage())
			.timestamp(msg.getTimestamp())
			.build();

		// Sink에 즉시 전송
		chatSinkManager.emitToRoom(msg.getRoomId(), dto);
		
		// Kafka는 후속 분산 처리를 위해 전송
		ProducerRecord<String, Object> chatRecord = new ProducerRecord<>(CHAT_SEND_TOPIC, dto);
		chatRecord.headers().add("__TypeId__", ChatMessageDTO.class.getName().getBytes(StandardCharsets.UTF_8));
		kafkaTemplate.send(chatRecord);
	}
	
	// 채팅 방 업데이트 전송
	public void updateChatRoom(ChatRoomRedisDto roomDto) {
		ProducerRecord<String, Object> redisRecord = new ProducerRecord<>(REDIS_UPDATE_TOPIC, roomDto);
		redisRecord.headers().add("__TypeId__", ChatRoomRedisDto.class.getName().getBytes(StandardCharsets.UTF_8));
		kafkaTemplate.send(redisRecord);
	}
}
