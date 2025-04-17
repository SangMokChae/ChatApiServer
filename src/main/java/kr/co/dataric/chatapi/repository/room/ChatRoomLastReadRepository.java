package kr.co.dataric.chatapi.repository.room;

import kr.co.dataric.chatapi.entity.room.ChatRoomLastRead;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface ChatRoomLastReadRepository extends ReactiveMongoRepository<ChatRoomLastRead, String> {
}
