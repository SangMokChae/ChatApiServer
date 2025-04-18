package kr.co.dataric.chatapi.controller.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.dataric.chatapi.config.sink.StatusSinkManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ChatStatusController {
	
	private final ReactiveRedisTemplate<String, String> redisTemplate;
	private final StatusSinkManager statusSinkManager;
	private final ObjectMapper objectMapper;
	
	@PostMapping("/api/chat/offline")
	public Mono<Void> markOffline(@RequestParam String roomId, @RequestParam String userId) {
		String key = "online:" +roomId +":" +userId;
		String message = toJson(Map.of("type", "online", "userId", userId));
		
		return redisTemplate.opsForValue()
			.set(key, "offline", Duration.ofMinutes(30))
			.doOnSuccess(v -> {
				log.info("사용자 offline 처리: {}", key);
				statusSinkManager.emit(roomId, message);
			})
			.then();
	}
	
	@PostMapping("/api/chat/online")
	public Mono<Void> markOnline(@RequestParam String roomId, @RequestParam String userId) {
		String key = "online:" +roomId +":" +userId;
		return redisTemplate.opsForValue()
			.set(key, "online", Duration.ofMinutes(30))
			.doOnSuccess(v -> log.info("사용자 Online 처리: {}", key))
			.then();
	}
	
	private String toJson(Object value) {
		try {
			return objectMapper.writeValueAsString(value);
		} catch (Exception e) {
			log.error("JSON 직렬화 실패", e);
			return "{}";
		}
	}
	
}
