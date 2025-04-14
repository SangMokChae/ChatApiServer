package kr.co.dataric.chatapi.kafka;

import kr.co.dataric.common.dto.ReadReceiptEvent;
import kr.co.dataric.chatapi.repository.room.ChatRoomRepository;
import kr.co.dataric.common.dto.ChatRoomRedisDto;
import kr.co.dataric.common.entity.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaChatProducer {

	private final ChatRoomRepository chatRoomRepository;
	private final KafkaTemplate<String, Object> kafkaTemplate;
	private static final String CHAT_TOPIC = "chat_messages";
	private static final String ROOM_UPDATE_TOPIC = "chat-room-update";
	
	public void sendMessage(ChatMessage message) {
		
		try {
			// ChatMessage 전송 - __TypeId__ 헤더 포함
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
			
			String roomId = message.getRoomId();

			// ChatListServer - ChatRoomListView 의 마지막 챗, 시간  업데이트 정보를 kafka로 전송
			chatRoomRepository.findByRoomId(message.getRoomId())
				.map(chatRoom -> ChatRoomRedisDto.builder()
					.roomId(message.getRoomId())
					.lastMessage(message.getMessage())
					.lastMessageTime(message.getTimestamp())
					.userIds(chatRoom.getParticipants())
					.build())
				.doOnNext(dto -> {
					ProducerRecord<String, Object> roomRecord = new ProducerRecord<>(ROOM_UPDATE_TOPIC, dto);
					roomRecord.headers().add("__TypeId__", "kr.co.dataric.common.dto.ChatRoomRedisDto".getBytes(StandardCharsets.UTF_8));
					kafkaTemplate.send(roomRecord);
				})
				.subscribe();
		} catch (Exception e) {
			log.error("Kafka 메시지 전송 실패", e);
		}
	}

	public void sendReadReceipt(String msgId, String userId, String roomId) {
		try {
			ReadReceiptEvent event = new ReadReceiptEvent(msgId, userId, roomId, LocalDateTime.now());
			
			ProducerRecord<String, Object> record = new ProducerRecord<>("read_receipt", event);
			record.headers().add("__TypeId__", "kr.co.dataric.common.dto.ReadReceiptEvent".getBytes(StandardCharsets.UTF_8));
			kafkaTemplate.send(record);
		} catch (Exception e) {
			log.error("Kafka 읽음 이벤트 전송 실패", e);
		}
	}
	
}
