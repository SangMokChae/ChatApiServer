package kr.co.dataric.chatapi.service.impl;

import kr.co.dataric.chatapi.entity.room.ChatRoomLastRead;
import kr.co.dataric.chatapi.repository.room.ChatRoomLastReadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomLastReadService {
	
	private final ChatRoomLastReadRepository repository;
	
	public Mono<Void> updateLastReadMessage(String roomId, String userId, String messageId) {
		return repository.findById(roomId)
			.defaultIfEmpty(ChatRoomLastRead.builder()
				.id(roomId)
				.roomId(roomId)
				.lastReadMap(new HashMap<>())
				.build())
			.flatMap(entity -> {
				entity.getLastReadMap().put(userId, messageId+"_"+LocalDateTime.now());
				return repository.save(entity);
			}).then();
	}
	
	public Mono<String> getLastReadMessage(String roomId, String userId) {
		return repository.findById(roomId)
			.flatMap(entity -> {
				String lastMsgId = entity.getLastReadMap() != null ? entity.getLastReadMap().get(userId) : null;
				return Mono.justOrEmpty(lastMsgId);
			})
			.switchIfEmpty(Mono.defer(() -> {
				log.info("No last read message found for roomId={}, userId={}", roomId, userId);
				return Mono.empty();
			}));
	}
	
	public Mono<Map<String, String>> getAllLastReadMessages(String roomId) {
		return repository.findById(roomId)
			.map(ChatRoomLastRead::getLastReadMap)
			.defaultIfEmpty(Collections.emptyMap());
	}
	
}
