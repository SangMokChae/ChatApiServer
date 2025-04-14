package kr.co.dataric.chatapi.filter.jwt;

import kr.co.dataric.common.jwt.provider.JwtProvider;
import kr.co.dataric.common.redis.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements WebFilter {
	
	private final JwtProvider jwtProvider;
	private final RedisService redisService;
	
	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		String path = exchange.getRequest().getURI().getPath();
		log.info("[AUTH] path: {}", path);
		
		// WebSocket 및 인증 예외 경로는 필터 제외
		if (path.startsWith("/ws/") || path.equals("/logout") || path.equals("/login") || path.equals("/loginProc")) {
			return chain.filter(exchange);
		}
		
		HttpCookie accessTokenCookie = exchange.getRequest().getCookies().getFirst("accessToken");
		HttpCookie refreshTokenCookie = exchange.getRequest().getCookies().getFirst("refreshToken");
		
		String accessToken = accessTokenCookie != null ? accessTokenCookie.getValue() : null;
		String refreshToken = refreshTokenCookie != null ? refreshTokenCookie.getValue() : null;
		
		// accessToken이 없거나 만료되었을 경우
		if (accessToken == null || jwtProvider.isTokenExpired(accessToken)) {
			String userId = null;
			if (accessToken != null) {
				userId = jwtProvider.extractUserIdIgnoreExpiration(accessToken);
			}
			if (userId == null && refreshToken != null) {
				userId = jwtProvider.extractUserIdIgnoreExpiration(refreshToken);
			}
			
			if (userId == null) {
				log.warn("❌ JWT 디코딩 실패: accessToken & refreshToken 모두 userId 추출 불가");
				exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
				return exchange.getResponse().setComplete();
			}
			
			String finalUserId = userId;
			return redisService.getRefreshToken(userId)
				.filter(saved -> saved.equals(refreshToken))
				.filter(valid -> !jwtProvider.isTokenExpired(refreshToken))
				.flatMap(valid -> {
					String newAccessToken = jwtProvider.createAccessToken(finalUserId);
					log.info("🔁 AccessToken 재발급 - userId: {}", finalUserId);
					
					ResponseCookie newAccessCookie = ResponseCookie.from("accessToken", newAccessToken)
						.httpOnly(true)
						.path("/")
						.maxAge(Duration.ofMinutes(30))
						.build();
					exchange.getResponse().addCookie(newAccessCookie);
					
					return authenticate(exchange, chain, newAccessToken);
				})
				.switchIfEmpty(unauthorized(exchange, "❌ refreshToken 불일치 또는 만료"));
		}
		
		return authenticate(exchange, chain, accessToken);
	}
	
	private Mono<Void> authenticate(ServerWebExchange exchange, WebFilterChain chain, String token) {
		String userId = jwtProvider.extractUserId(token);
		if (userId == null) {
			return unauthorized(exchange, "❌ accessToken → userId 추출 실패");
		}
		
		UsernamePasswordAuthenticationToken auth =
			new UsernamePasswordAuthenticationToken(userId, null, List.of());
		
		return chain.filter(exchange)
			.contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth))
			.doOnSuccess(success -> log.debug("✅ 인증 완료 - userId: {}", userId));
	}
	
	private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
		log.warn("[AUTH] 인증 실패: {}", message);
		exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
		return exchange.getResponse().setComplete();
	}
}