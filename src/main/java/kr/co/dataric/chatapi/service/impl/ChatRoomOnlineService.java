package kr.co.dataric.chatapi.service.impl;

import kr.co.dataric.chatapi.entity.onoff.ChatRoomOnline;
import kr.co.dataric.chatapi.repository.room.ChatRoomOnlineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomOnlineService {
	
	private final ChatRoomOnlineRepository onlineRepository;
	
	// user 추가
	public Mono<Void> addUserToOnline(String roomId, String userId) {
		return onlineRepository.findById(roomId)
			.defaultIfEmpty(ChatRoomOnline.builder()
				.roomId(roomId)
				.onlineUsers(new ArrayList<>())
				.build())
			.flatMap(room -> {
				if (!room.getOnlineUsers().contains(userId)) {
					room.getOnlineUsers().add(userId);
				}
				return onlineRepository.save(room);
			}).then();
	}
	
	public Mono<Void> removeUserFromOnline(String roomId, String userId) {
		return onlineRepository.findById(roomId)
			.flatMap(room -> {
				room.getOnlineUsers().remove(userId);
				return onlineRepository.save(room);
			}).then();
	}
	
	public Mono<List<String>> getOnlineUsers(String roomId) {
		return onlineRepository.findById(roomId)
			.map(ChatRoomOnline::getOnlineUsers)
			.defaultIfEmpty(Collections.emptyList());
	}
	
}
