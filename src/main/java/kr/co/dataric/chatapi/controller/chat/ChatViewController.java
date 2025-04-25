package kr.co.dataric.chatapi.controller.chat;

import kr.co.dataric.chatapi.dto.request.view.ViewRequestDto;
import kr.co.dataric.common.jwt.provider.JwtProvider;
import kr.co.dataric.common.redis.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;


@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatViewController {
	
	private final JwtProvider jwtProvider;
	private final ReactiveRedisTemplate<String, String> redisTemplate;
	
	@PostMapping("/view/chatView")
	public Mono<Rendering> enterChatView(
		@ModelAttribute ViewRequestDto viewRequestDto, // 415 문제로 인해 @RequestBody 가 아닌 @ModelAttribute로 변경 --> @RequestBody는 JSON을 기대하는데 form은 x-www-form-urlencoded로 보냄
		@CookieValue(name = "accessToken", required = false) String token
	) {
		String userId = jwtProvider.extractUserIdIgnoreExpiration(token);
		String roomId = viewRequestDto.getRoomId();
		
		if (userId == null || viewRequestDto.getRoomId() == null || roomId.isBlank()) {
			return Mono.just(Rendering.redirectTo("/login").build());
		}
		
		String redisKey = "online:" +viewRequestDto.getRoomId() +":" +userId;
		
		redisTemplate.opsForValue()
			.set(redisKey, "online", Duration.ofMinutes(30)) // TTL로 누락 방지
			.doOnSuccess(res -> log.info("입장 시 online 처리: {}", redisKey))
			.subscribe();

		String keyPattern = "last_read:" + roomId +":*";
		
		Mono<Map<String, String>> lastReadMono = redisTemplate.scan(ScanOptions.scanOptions().match(keyPattern).count(100).build())
			.flatMap(key -> redisTemplate.opsForValue().get(key)
				.filter(Objects::nonNull) // null 방지
				.map(value -> {
					String[] parts = key.split(":"); // key 기준으로 split 해야함
					String uid = parts.length >= 3 ? parts[2] : "unknown"; // userId 위치
					return Map.entry(uid, value);
				})) // userId 추출
			.collectMap(Map.Entry::getKey, Map.Entry::getValue);
		
		return lastReadMono.map(lastReadMap ->
			Rendering.view("chat/chatView")
					.modelAttribute("roomId", viewRequestDto.getRoomId())
					.modelAttribute("userId", userId)
					.modelAttribute("participants", viewRequestDto.getParticipants())
					.modelAttribute("lastRead", lastReadMap)
					.build()
		);
	}
}