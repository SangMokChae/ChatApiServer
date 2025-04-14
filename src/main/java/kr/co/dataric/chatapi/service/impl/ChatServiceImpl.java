package kr.co.dataric.chatapi.service.impl;

import kr.co.dataric.chatapi.kafka.KafkaChatProducer;
import kr.co.dataric.chatapi.repository.chat.ChatMessageRepository;
import kr.co.dataric.chatapi.service.ChatService;
import kr.co.dataric.common.entity.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

// ChatServiceImpl.java 또는 Kafka 메시지 전송 직후 처리
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {
	
	private final ChatMessageRepository chatMessageRepository;
	
	@Override
	public Flux<ChatMessage> getMessagesByRoom(String roomId, int offset, int size) {
		return chatMessageRepository.findByRoomIdOrderByTimestampDesc(roomId)
			.skip(offset)
			.take(size);
	}
	
	@Override
	public Mono<Void> saveChatMessage(ChatMessage message) {
		return chatMessageRepository.save(message).then();
	}
}
