package kr.co.dataric.chatapi.controller.login;

import kr.co.dataric.chatapi.dto.request.login.LoginRequest;
import kr.co.dataric.chatapi.repository.user.UserRepository;
import kr.co.dataric.common.jwt.provider.JwtProvider;
import kr.co.dataric.common.redis.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Controller
@RequiredArgsConstructor
public class LoginController {
	
	private final UserRepository userRepository;
	private final JwtProvider jwtProvider;
	private final RedisService redisService;
	private final PasswordEncoder passwordEncoder;
	
	@GetMapping("/favicon.ico")
	public Mono<Void> favicon() {
		return Mono.empty(); // 혹은 특정 favicon 반환
	}
	
	/**
	 * Gateway → ChatApiServer root 접근 시 login으로 redirect
	 */
	@GetMapping("/")
	public Mono<Rendering> redirectToLogin() {
		return Mono.just(Rendering.redirectTo("/login").build());
	}
	
	/**
	 * 로그인 화면 렌더링
	 */
	@GetMapping("/login")
	public Mono<Rendering> loginPage(@RequestParam(value = "code", required = false) String code,
																	 @CookieValue(name = "accessToken", required = false) String accessToken,
																	 @CookieValue(name = "refreshToken", required = false) String refreshToken,
																	 ServerWebExchange exchange) {
		// 1. AccessToken 유효한 경우 -> 바로 /view/ChatListView 이동
		if (accessToken != null && !jwtProvider.isTokenExpired(accessToken)) {
			String userId = jwtProvider.extractUserId(accessToken);
			if (userId != null) {
				return Mono.just(Rendering.redirectTo("/view/chatListView").build());
			}
		}
		
		// 2. AccessToken 만료 or 없음 -> refreshToken 검사
		if (refreshToken != null) {
			String userId = jwtProvider.extractUserIdIgnoreExpiration(refreshToken);
			if (userId != null && !jwtProvider.isTokenExpired(refreshToken)) {
				return redisService.getRefreshToken(userId)
					.filter(saved -> saved.equals(refreshToken))
					.flatMap(valid -> {
						// 2-1. refreshToken 유효 -> accessToken 재발급 및 쿠키 저장 후 Redirection
						String newAccessToken = jwtProvider.createAccessToken(userId);
						ResponseCookie newAccessCookie = ResponseCookie.from("accessToken", newAccessToken)
							.httpOnly(true)
							.path("/")
							.maxAge(Duration.ofMinutes(30))
							.build();
						exchange.getResponse().addCookie(newAccessCookie);
						
						return Mono.just(Rendering.redirectTo("/view/chatListView").build());
					});
			}
		}
		
		// 3. 둘다 유효하지 않으면 -> login 렌더링
		return Mono.just(Rendering.view("/login/login").modelAttribute("code", code != null).build());
	}
	
	/**
	 * 로그인 처리 로직
	 */
	@PostMapping("/loginProc")
	public Mono<Rendering> loginProc(@ModelAttribute LoginRequest loginRequest, ServerWebExchange exchange) {
		String username = loginRequest.getUserId();
		String password = loginRequest.getPassword();
		
		return userRepository.findByUsername(username)
			.flatMap(user -> {
				if (!passwordEncoder.matches(password, user.getPassword())) {
					log.warn("❌ Login Failed - 잘못된 비밀번호");
					return Mono.just(Rendering.redirectTo("/login?code=wrongPs").build());
				}
				
				// ✅ 토큰 생성
				String accessToken = jwtProvider.createAccessToken(username);
				String refreshToken = jwtProvider.createRefreshToken(username);
				
				// ✅ Redis 저장 (refreshToken)
				redisService.saveRefreshToken(username, refreshToken).subscribe();
				
				// ✅ 쿠키 설정
				ResponseCookie accessTokenCookie = ResponseCookie.from("accessToken", accessToken)
					.httpOnly(true)
					.path("/")
					.maxAge(Duration.ofMinutes(30))
					.build();
				
				ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", refreshToken)
					.httpOnly(true)
					.path("/")
					.maxAge(Duration.ofDays(7))
					.build();
				
				exchange.getResponse().addCookie(accessTokenCookie);
				exchange.getResponse().addCookie(refreshTokenCookie);
				
				log.info("✅ Login Success : userId = {}", username);
				
				return Mono.just(Rendering.redirectTo("/view/chatListView").build());
			})
			.switchIfEmpty(Mono.defer(() -> {
				log.warn("❌ Login Failed - 존재하지 않는 사용자");
				return Mono.just(Rendering.redirectTo("/login?code=loginFail").build());
			}));
	}
}
