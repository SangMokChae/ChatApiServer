package kr.co.dataric.chatapi.subscriber;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.dataric.chatapi.handler.ReadWebSocketHandler;
import kr.co.dataric.chatapi.handler.ReadStatusWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatRedisSubscriber implements MessageListener {
	
	private final ObjectMapper objectMapper;
	private final ReadStatusWebSocketHandler readStatusWebSocketHandler;
	private final ReadWebSocketHandler readWebSocketHandler;
	private final ReactiveRedisTemplate<String, String> redisTemplate;
	
	@Override
	public void onMessage(Message message, byte[] pattern) {
		String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
		String json = new String(message.getBody(), StandardCharsets.UTF_8);

		try {
			if ("onlineUpdate".equals(channel)) {
				Map<String, String> payload = objectMapper.readValue(json, new TypeReference<>() {});
				String roomId = payload.get("roomId");
				String userId = payload.get("userId");
				String status = payload.get("status");
				
				log.info("Online 상태 변경 수신: roomId={}, userId={}, status={}", roomId, userId, status);
				
				readStatusWebSocketHandler.broadcastUserStatus(roomId, userId, status);
			} else if("chatReadUpdate".equals(channel)) {
				Map<String, String> payload = objectMapper.readValue(json, new TypeReference<>() {});
				String roomId = payload.get("roomId");
				String userId = payload.get("userId");
				
				String keyPattern = "last_read:" + roomId +":*";
				
				redisTemplate.scan(ScanOptions.scanOptions().match(keyPattern).count(100).build())
					.flatMap(key -> redisTemplate.opsForValue().get(key)
						.filter(Objects::nonNull)
						.map(value -> {
							String[] parts = key.split(":");
							String uid = parts.length >= 3 ? parts[2] : "unknown";
							
							String[] msgParts = value.split("_", 2);
							String msgId = msgParts.length > 0 ? msgParts[0] : "INITIAL";
							String timestamp = msgParts.length > 1 ? msgParts[1] : "1970-01-01T00:00:00.123";
							
							return Map.of(
								"userId", uid,
								"msgId", msgId,
								"timestamp", timestamp
							);
						}))
					.collectList()
					.doOnNext(userReadList -> {
						log.info("전체 userRedisList 전송: {}", userReadList);
						readStatusWebSocketHandler.broadcastUserRead(roomId, userId, userReadList);
					})
					.subscribe();
			}
		} catch (Exception e) {
			log.error("Online 상태 수신 처리 실패");
		}
		
	}
}
