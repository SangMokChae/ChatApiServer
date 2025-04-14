package kr.co.dataric.common.jwt.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import kr.co.dataric.common.redis.service.RedisService;
import kr.co.dataric.common.jwt.entity.JwtProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtProvider {
	
	private final RedisService redisService;
	private final JwtProperties jwtProperties;
	
	private SecretKey key;
	
	@PostConstruct
	public void init() {
		this.key = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
	}
	
	public String createAccessToken(String username) {
		Date now = new Date();
		Date expiryDate = new Date(now.getTime() + jwtProperties.getAccessTokenExpiration());
		
		return Jwts.builder()
			.subject(username)
			.issuedAt(now)
			.expiration(expiryDate)
			.signWith(key)
			.compact();
	}
	
	public String createRefreshToken(String username) {
		Date now = new Date();
		Date expiryDate = new Date(now.getTime() + jwtProperties.getRefreshTokenExpiration());
		
		return Jwts.builder()
			.subject(username)
			.issuedAt(now)
			.expiration(expiryDate)
			.signWith(key)
			.compact();
	}
	
	public String refreshAccessToken(String refreshToken) {
		String username = extractUserId(refreshToken);
		
		if (username == null || isTokenExpired(refreshToken)) {
			throw new RuntimeException("유효하지 않거나 만료된 Refresh Token입니다.");
		}
		
		return redisService.getRefreshToken(username)
			.flatMap(savedToken -> {
				if (savedToken.equals(refreshToken)) {
					String newAccessToken = createAccessToken(username);
					log.info("사용자 {}에 대한 AccessToken 재발급 완료", username);
					return Mono.just(newAccessToken);
				} else {
					return Mono.error(new RuntimeException("저장된 Refresh Token과 일치하지 않습니다."));
				}
			}).block();
	}
	
	public String extractUserId(String token) {
		try {
			Claims claims = (Claims) Jwts.parser()
				.verifyWith(key)
				.build()
				.parse(token)
				.getPayload();
			return claims.getSubject();
		} catch (JwtException e) {
			log.error("JWT 검증 실패: {}", e.getMessage());
			return null;
		}
	}
	
	public boolean isTokenExpired(String token) {
		try {
			Claims claims = (Claims) Jwts.parser()
				.verifyWith(key)
				.build()
				.parse(token)
				.getPayload();
			return claims.getExpiration().before(new Date());
		} catch (JwtException e) {
			return true;
		}
	}
	
	public static String extractUserIdFromTokenWithoutValidation(String token) {
		try {
			String[] parts = token.split("\\.");
			if (parts.length >= 2) {
				String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
				ObjectMapper mapper = new ObjectMapper();
				return mapper.readTree(payloadJson).get("sub").asText();
			}
		} catch (Exception e) {
			log.error("로그아웃용 UserId 추출 실패 : {}", e.getMessage());
		}
		return null;
	}
	
	public String extractUserIdIgnoreExpiration(String token) {
		try {
			Claims claims = (Claims) Jwts.parser()
				.verifyWith(key)
				.build()
				.parse(token)
				.getPayload();
			return claims.getSubject();
		} catch (Exception e) {
			log.warn("JWT 디코딩 실패: {}", e.getMessage());
			return null;
		}
	}
	
	public boolean validate(String token) {
		return extractUserId(token) != null;
	}
}
