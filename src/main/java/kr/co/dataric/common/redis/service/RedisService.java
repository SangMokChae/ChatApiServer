package kr.co.dataric.common.redis.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService {
	
	private final ReactiveStringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;
	
	private static final String REFRESH_TOKEN_PREFIX = "refreshToken:";
	private static final String ACCESS_TOKEN_PREFIX = "accessToken:";
	
	// ✅ ChatApiServer, ChatGateway, ChatListServer, KafkaConsumer 공통 사용
	public Mono<String> getRefreshToken(String username) {
		return redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + username);
	}
	
	// ✅ ChatApiServer 전용
	public Mono<Boolean> saveRefreshToken(String username, String refreshToken) {
		String key = REFRESH_TOKEN_PREFIX + username;
		return redisTemplate.opsForValue()
			.set(key, refreshToken, Duration.ofDays(7));
	}
	
	// ✅ ChatApiServer 전용
	public Mono<Boolean> deleteRefreshToken(String userId) {
		return redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId)
			.map(deleted -> deleted > 0);
	}
	
	// ✅ ChatListServer 전용: 채팅방 정보 Redis 저장
	public <T> Mono<Boolean> set(String key, T value) {
		try {
			String json = objectMapper.writeValueAsString(value);
			return redisTemplate.opsForValue().set(key, json);
		} catch (JsonProcessingException e) {
			log.error("❌ Redis 저장 실패 - 직렬화 오류", e);
			return Mono.just(false);
		}
	}
	
	// ✅ ChatListServer 전용: TTL 포함 Redis 저장
	public <T> Mono<Boolean> set(String key, T value, Duration ttl) {
		try {
			String json = objectMapper.writeValueAsString(value);
			return redisTemplate.opsForValue().set(key, json, ttl);
		} catch (JsonProcessingException e) {
			log.error("❌ Redis TTL 저장 실패 - 직렬화 오류", e);
			return Mono.just(false);
		}
	}
	
	// ✅ ChatListServer 전용: Redis 조회 및 역직렬화
	public <T> Mono<T> get(String key, Class<T> clazz) {
		ReactiveValueOperations<String, String> ops = redisTemplate.opsForValue();
		return ops.get(key)
			.flatMap(json -> {
				try {
					return Mono.justOrEmpty(objectMapper.readValue(json, clazz));
				} catch (JsonProcessingException e) {
					log.error("❌ Redis 역직렬화 실패 - key: {}", key, e);
					return Mono.empty();
				}
			});
	}
	
	// ✅ 공용 삭제
	public Mono<Boolean> delete(String key) {
		return redisTemplate.delete(key).map(count -> count > 0);
	}
	
	// ✅ Kafka 읽음 처리용: 읽음 사용자 저장 + TTL (10분)
	public void addToSet(String key, String userId) {
		redisTemplate.opsForSet().add(key, userId);
		redisTemplate.expire(key, Duration.ofMinutes(10));
	}
	
	// ✅ 읽은 사용자 목록 조회
	public Flux<String> getSetMembers(String key) {
		return redisTemplate.opsForSet().members(key);
	}
	
	// ✅ 키 삭제
	public Mono<Void> deleteKey(String key) {
		return redisTemplate.delete(key).then();
	}
	
	// ✅ 읽은 사람 호출
	public Mono<Long> getSetSize(String key) {
		return redisTemplate.opsForSet().size(key);
	}
	
	public Flux<String> scanKeys(String pattern) {
		return redisTemplate
			.getConnectionFactory()
			.getReactiveConnection()
			.keyCommands()
			.scan(ScanOptions.scanOptions().match(pattern).count(1000).build())
			.map(byteBuffer -> StandardCharsets.UTF_8.decode(byteBuffer).toString());
	}
}
