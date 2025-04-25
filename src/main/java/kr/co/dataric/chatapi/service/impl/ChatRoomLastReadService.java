package kr.co.dataric.chatapi.service.impl;

import kr.co.dataric.chatapi.entity.room.ChatRoomLastRead;
import kr.co.dataric.chatapi.repository.room.ChatRoomLastReadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomLastReadService {
	
	private final ChatRoomLastReadRepository repository;
	private final ReactiveStringRedisTemplate redisTemplate;
	
	public Mono<Void> syncAllLastReadFromRedisToMongo(String roomId) {
		String keyPattern = "last_read:" + roomId + ":*";
		
		return redisTemplate.scan(ScanOptions.scanOptions().match(keyPattern).count(100).build())
			.flatMap(key -> redisTemplate.opsForValue().get(key)
				.filter(Objects::nonNull)
				.map(value -> {
					String[] parts = key.split(":");
					String uid = parts.length >= 3 ? parts[2] : "unknown";
					return Map.entry(uid, value);
				}))
			.collectList()
			.flatMap(entries -> {
				return repository.findById(roomId)
					.defaultIfEmpty(ChatRoomLastRead.builder()
						.id(roomId)
						.roomId(roomId)
						.lastReadMap(new HashMap<>())
						.build())
					.flatMap(entity -> {
						entries.forEach(e -> entity.getLastReadMap().put(e.getKey(), e.getValue()));
						return repository.save(entity);
					});
			})
			.doOnSuccess(rs -> log.info("✅ Redis → Mongo 동기화 완료 roomId={}, entries={}", roomId, rs.getLastReadMap().size()))
			.then();
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
