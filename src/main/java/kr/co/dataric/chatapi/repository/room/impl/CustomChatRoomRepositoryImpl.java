package kr.co.dataric.chatapi.repository.room.impl;

import kr.co.dataric.chatapi.repository.room.CustomChatRoomRepository;
import kr.co.dataric.common.entity.ChatMessage;
import kr.co.dataric.common.entity.ChatRoom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Slf4j
@Repository
@RequiredArgsConstructor
public class CustomChatRoomRepositoryImpl implements CustomChatRoomRepository {

	private final ReactiveMongoTemplate mongoTemplate;

	@Override
	public Mono<Void> updateLastMessage(String roomId, String message, LocalDateTime chatDate) {
		Query query = Query.query(Criteria.where("roomId").is(roomId));
		Update update = new Update()
			.set("lastMessage", message)
			.set("lastMessageTime", chatDate);
		
		return mongoTemplate.updateFirst(query, update, ChatRoom.class).then();
	}
	
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
