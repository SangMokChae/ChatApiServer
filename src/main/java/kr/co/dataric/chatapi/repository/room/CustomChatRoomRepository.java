package kr.co.dataric.chatapi.repository.room;

import reactor.core.publisher.Mono;

import java.util.List;

public interface CustomChatRoomRepository {
	Mono<Void> createNewChatRoom(String roomId, List<String> userIdsList);
}
