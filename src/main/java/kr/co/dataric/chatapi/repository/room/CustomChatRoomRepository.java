package kr.co.dataric.chatapi.repository.room;

import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

public interface CustomChatRoomRepository {
	Mono<Void> updateLastMessage(String roomId, String message, LocalDateTime chatDate);
	
	Mono<Void> createNewChatRoom(String roomId, List<String> userIdsList);
}
