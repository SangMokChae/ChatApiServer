package kr.co.dataric.chatapi.config.sink;

import kr.co.dataric.common.dto.ChatMessageDTO;
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
	
	// userId 기준 다중 세션 Sink 관리
	private final Map<String, Set<Sinks.Many<ChatMessageDTO>>> userSinkMap = new ConcurrentHashMap<>();
	
	// ✅ roomId 기준 참여자 userId 목록 관리 (채팅방 전체 broadcast 용도)
	private final Map<String, Set<String>> roomUserMap = new ConcurrentHashMap<>();
	
	/**
	 * 유저에 대해 Sink 등록 및 room 참여 등록
	 */
	public Sinks.Many<ChatMessageDTO> register(String roomId, String userId) {
		// userId -> Sink 저장
		Sinks.Many<ChatMessageDTO> sink = Sinks.many().multicast().directBestEffort();
		userSinkMap.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(sink);
		
		// roomId -> userId 참여자 등록
		roomUserMap.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(userId);
		
		log.info("✅ Sink 등록: userId={}, roomId={}, 총 Sink 수: {}", userId, roomId, userSinkMap.get(userId).size());
		return sink;
	}
	
	/**
	 * 유저 Sink 제거 및 room 참여 해제
	 */
	public void unregister(String roomId, String userId, Sinks.Many<ChatMessageDTO> sink) {
		Set<Sinks.Many<ChatMessageDTO>> sinks = userSinkMap.get(userId);
		if (sinks != null) {
			sinks.remove(sink);
			if (sinks.isEmpty()) {
				userSinkMap.remove(userId);
				log.info("❎ userId 모든 Sink 제거 완료: {}", userId);
			}
		}
		
		Set<String> roomUsers = roomUserMap.get(roomId);
		if (roomUsers != null) {
			roomUsers.remove(userId);
			if (roomUsers.isEmpty()) {
				roomUserMap.remove(roomId);
				log.info("❎ roomId={} 모든 사용자 제거", roomId);
			}
		}
	}
	
	/**
	 * userId에게 메시지 전송 (모든 세션)
	 */
	public void emitToUser(String userId, ChatMessageDTO message) {
		Set<Sinks.Many<ChatMessageDTO>> sinks = userSinkMap.get(userId);
		if (sinks != null && !sinks.isEmpty()) {
			sinks.forEach(sink -> sink.tryEmitNext(message));
		}
	}
	
	/**
	 * 해당 채팅방의 전체 참여자에게 메시지 전송
	 */
	public void emitToRoom(String roomId, ChatMessageDTO message) {
		Set<String> users = roomUserMap.get(roomId);
		if (users != null) {
			users.forEach(userId -> emitToUser(userId, message));
		}
	}
	
	/**
	 * 현재 접속 중인 userId인지 여부 확인
	 */
	public boolean isUserConnected(String userId) {
		return userSinkMap.containsKey(userId) && !userSinkMap.get(userId).isEmpty();
	}
	
	/**
	 * 특정 방의 현재 참여자 수 반환
	 */
	public int getParticipantCount(String roomId) {
		Set<String> users = roomUserMap.get(roomId);
		return users != null ? users.size() : 0;
	}
}
