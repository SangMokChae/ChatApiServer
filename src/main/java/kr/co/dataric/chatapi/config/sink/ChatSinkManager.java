package kr.co.dataric.chatapi.config.sink;

import kr.co.dataric.common.entity.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class ChatSinkManager {
	
	// 채팅방별 Sink
	private final Map<String, Sinks.Many<ChatMessage>> roomSinkMap = new ConcurrentHashMap<>();
	
	// 채팅방별 구독 중인 사용자 수
	// Set을 사용함으로써 반복 X
	private final Map<String, Set<String>> roomSubscribers = new ConcurrentHashMap<>();
	
	/**
	 * Sink 생성 또는 재사용
	 */
	public Sinks.Many<ChatMessage> getOrCreateSink(String roomId, String userId) {
		roomSubscribers.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(userId);
		return roomSinkMap.computeIfAbsent(roomId, k -> {
			log.info("✅ 채팅방 Sink 생성: {}", roomId);
			return Sinks.many().multicast().directBestEffort();
		});
	}
	
	/**
	 * Sink 제거 (마지막 유저 퇴장 시에만)
	 */
	public void removeSubscriber(String roomId, String userId) {
		Set<String> subscribers = roomSubscribers.get(roomId);
		if (subscribers != null) {
			subscribers.remove(userId);
			if (subscribers.isEmpty()) {
				log.info("❎ 채팅방 Sink 제거: {} (마지막 유저 퇴장)", roomId);
				roomSubscribers.remove(roomId);
				roomSinkMap.remove(roomId);
			}
		}
	}
	
	/**
	 * 메시지 전송
	 */
	public void emitToRoom(String roomId, ChatMessage message) {
		Sinks.Many<ChatMessage> sink = roomSinkMap.get(roomId);
		if (sink != null) sink.tryEmitNext(message);
	}
	
	/**
	 * 해당 room의 접속자 수 반환
	 */
	public int returnRoomsUsers(String roomId) {
		return roomSubscribers.get(roomId).size();
	}
}
