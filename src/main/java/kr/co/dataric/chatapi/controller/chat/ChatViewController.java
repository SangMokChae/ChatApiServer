package kr.co.dataric.chatapi.controller.chat;

import kr.co.dataric.chatapi.dto.request.view.ViewRequestDto;
import kr.co.dataric.common.jwt.provider.JwtProvider;
import kr.co.dataric.common.redis.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;

import java.time.Duration;


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
		
		if (userId == null || viewRequestDto.getRoomId() == null || viewRequestDto.getRoomId().isBlank()) {
			return Mono.just(Rendering.redirectTo("/login").build());
		}
		
		String redisKey = "online:" +viewRequestDto.getRoomId() +":" +userId;
		Mono<Boolean> markOnline = redisTemplate.opsForValue()
			.set(redisKey, "online", Duration.ofMinutes(30)) // TTL로 누락 방지
			.doOnSuccess(res -> log.info("입장 시 online 처리: {}", redisKey));
		
		return Mono.just(Rendering.view("chat/chatView")
			.modelAttribute("roomId", viewRequestDto.getRoomId())
			.modelAttribute("userId", userId)
			.modelAttribute("inUserIds", viewRequestDto.getInUserIds())
			.build());
	}
}