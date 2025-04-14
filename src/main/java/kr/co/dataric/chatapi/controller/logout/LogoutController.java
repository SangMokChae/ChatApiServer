package kr.co.dataric.chatapi.controller.logout;

import kr.co.dataric.common.jwt.provider.JwtProvider;
import kr.co.dataric.common.redis.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Controller
@RequiredArgsConstructor
public class LogoutController {
	
	private final RedisService redisService;
	
	@GetMapping("/logout")
	public Mono<Rendering> logout(ServerWebExchange exchange) {
		log.info("exchange :: {}", exchange);
		HttpCookie accessTokenCookie = exchange.getRequest().getCookies().getFirst("accessToken");
		
		if (accessTokenCookie != null) {
			String accessToken = accessTokenCookie.getValue();
			String userId = JwtProvider.extractUserIdFromTokenWithoutValidation(accessToken);
			
			if (userId != null) {
				log.info("Redis refreshToken 삭제 - userId : {}", userId);
				redisService.deleteRefreshToken(userId).subscribe();
			}
		}
		
		// accessToken 쿠키 제거
		ResponseCookie deleteAccessCookie = ResponseCookie.from("accessToken", "")
			.path("/")
			.httpOnly(true)
			.maxAge(0)
			.build();
		
		// refreshToken 쿠키 제거
		ResponseCookie deleteRefreshCookie = ResponseCookie.from("refreshToken", "")
				.path("/")
				.httpOnly(true)
				.maxAge(0)
				.build();
		
		exchange.getResponse().addCookie(deleteAccessCookie);
		exchange.getResponse().addCookie(deleteRefreshCookie);
		
		return Mono.just(Rendering.redirectTo("/login").build());
	}

}
