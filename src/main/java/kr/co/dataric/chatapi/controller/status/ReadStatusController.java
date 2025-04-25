package kr.co.dataric.chatapi.controller.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.dataric.chatapi.config.sink.StatusSinkManager;
import kr.co.dataric.chatapi.dto.request.read.StatusRequestDto;
import kr.co.dataric.chatapi.handler.HandlerSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ReadStatusController {
	
	private final ReactiveRedisTemplate<String, String> redisTemplate;
	private final HandlerSupport handlerSupport;
	
	@PostMapping("/api/chat/offline")
	public Mono<Void> markOffline(@RequestBody StatusRequestDto request) {
		String key = "online:" +request.getRoomId() + ":" +request.getUserId();
		
		return redisTemplate.opsForValue()
			.set(key, "offline", Duration.ofMinutes(30))
			.doOnSuccess(v -> log.info("사용자 offline 처리: {}", key))
			.then(redisTemplate.convertAndSend("onlineUpdate", handlerSupport.toJson(
				Map.of(
					"roomId", request.getRoomId(),
					"userId", request.getUserId(),
					"status", "offline",
					"type", "status"
				))))
			.then(saveLastRead(request.getRoomId(), request.getUserId(), request.getLastRead()));
	}
	
	@PostMapping("/api/chat/online")
	public Mono<Void> markOnline(@RequestBody StatusRequestDto request) {
		String key = "online:" +request.getRoomId() +":" +request.getUserId();
		
		log.info("roomId:{}, userId:{}, lastRead:{}", request.getRoomId(), request.getUserId(), request.getLastRead());
		
		return redisTemplate.opsForValue()
			.set(key, "online", Duration.ofMinutes(30))
			.doOnSuccess(v -> log.info("사용자 Online 처리: {}", key))
			.then(redisTemplate.convertAndSend("onlineUpdate", handlerSupport.toJson(
				Map.of(
					"roomId", request.getRoomId(),
					"userId", request.getUserId(),
					"status", "online",
					"type", "status"
				))))
			.then(saveLastRead(request.getRoomId(), request.getUserId(), request.getLastRead()));
	}
	
	@GetMapping("/api/chat/lastRead")
	public Mono<Map<String, String>> getLastReadAll(@RequestParam String roomId) {
		String keyPattern = "last_read:" + roomId +":*";
		
		return redisTemplate.scan(ScanOptions.scanOptions().match(keyPattern).count(100).build())
			.flatMap(key -> redisTemplate.opsForValue().get(key)
			.filter(Objects::nonNull) // null 방지
			.map(value -> {
				String[] parts = key.split(":"); // key 기준으로 split 해야함
				String uid = parts.length >= 3 ? parts[2] : "unknown"; // userId 위치
				return Map.entry(uid, value);
			})) // userId 추출
			.collectMap(Map.Entry::getKey, Map.Entry::getValue);
	}
	
	private Mono<Void> saveLastRead(String roomId, String userId, String lastRead) {
		if (lastRead == null) return Mono.empty();
		String key = "last_read:" +roomId +":" +userId;
		return redisTemplate.opsForValue()
			.set(key, lastRead, Duration.ofDays(30))
			.then(redisTemplate.convertAndSend("chatReadUpdate", handlerSupport.toJson(
				Map.of(
					"roomId", roomId,
					"userId", userId
				))))
			.then();
	}
}
