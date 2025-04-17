package kr.co.dataric.chatapi.repository.room.impl;

import kr.co.dataric.chatapi.repository.room.CustomChatRoomRepository;
import kr.co.dataric.common.entity.ChatRoom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class CustomChatRoomRepositoryImpl implements CustomChatRoomRepository {

	private final ReactiveMongoTemplate mongoTemplate;
	
	@Override
	public Mono<Void> createNewChatRoom(String roomId, List<String> userIdsList) {
		ChatRoom room = ChatRoom.builder()
			.roomId(roomId)
			.participants(userIdsList)
			.createAt(LocalDateTime.now())
			.roomType("1")
			.roomTypeKey("null")
			.roomName("")
			.build();

		return mongoTemplate.insert(room).then();
	}
}
