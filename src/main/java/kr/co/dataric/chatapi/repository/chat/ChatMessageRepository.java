package kr.co.dataric.chatapi.repository.chat;

import kr.co.dataric.common.entity.ChatMessage;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ChatMessageRepository extends ReactiveMongoRepository<ChatMessage, String> {
	Flux<ChatMessage> findByRoomIdOrderByTimestampDesc(String roomId);
	
}
