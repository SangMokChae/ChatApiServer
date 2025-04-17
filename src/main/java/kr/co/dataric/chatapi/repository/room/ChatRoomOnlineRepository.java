package kr.co.dataric.chatapi.repository.room;

import kr.co.dataric.chatapi.entity.onoff.ChatRoomOnline;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface ChatRoomOnlineRepository extends ReactiveMongoRepository<ChatRoomOnline, String> {
}
