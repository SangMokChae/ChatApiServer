package kr.co.dataric.chatapi.repository.room;

import kr.co.dataric.common.entity.ChatRoom;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;


public interface ChatRoomRepository extends ReactiveMongoRepository<ChatRoom, String> {
	Flux<ChatRoom> findByRoomId(String roomId);
}
