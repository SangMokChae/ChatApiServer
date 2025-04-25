package kr.co.dataric.chatapi.config.sink;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class StatusSinkManager {
	
	private final Map<String, Set<Sinks.Many<String>>> sinkMap = new ConcurrentHashMap<>();
	
	/**
	 * ✅ Sink 등록: roomId 별로 Sink Set에 추가
	 */
	public void register(String roomId, Sinks.Many<String> sink) {
		Set<Sinks.Many<String>> sinks = getOrCreate(roomId);
		sinks.add(sink);
		log.info("✅ Sink 등록 - roomId: {}, 현재 연결 수: {}", roomId, sinks.size());
	}
	
	/**
	 * ✅ Sink 제거: 해당 roomId의 Sink Set에서 제거
	 */
	public void remove(String roomId, Sinks.Many<String> sink) {
		Set<Sinks.Many<String>> sinks = sinkMap.get(roomId);
		if (sinks != null) {
			sinks.remove(sink);
			log.info("🧹 Sink 제거 - roomId: {}, 남은 Sink 수: {}", roomId, sinks.size());
			if (sinks.isEmpty()) {
				sinkMap.remove(roomId);
				log.info("🗑️ roomId '{}' 의 Sink Set 제거 완료", roomId);
			}
		}
	}
	
	/**
	 * ✅ Sink로 메시지 브로드캐스트 전송
	 */
	public void emit(String roomId, String message) {
		Set<Sinks.Many<String>> sinks = sinkMap.get(roomId);
		if (sinks == null || sinks.isEmpty()) {
			log.debug("⚠️ 전송할 Sink 없음 - roomId: {}", roomId);
			return;
		}
		
		sinks.forEach(sink -> {
			Sinks.EmitResult result = sink.tryEmitNext(message);
			if (result.isFailure()) {
				log.warn("❌ 메시지 전송 실패 - 이유: {}, 메시지: {}", result, message);
			}
		});
	}
	
	/**
	 * ✅ Sink Set 조회 (읽기 전용)
	 */
	public Set<Sinks.Many<String>> get(String roomId) {
		return sinkMap.getOrDefault(roomId, Collections.emptySet());
	}
	
	/**
	 * ✅ Sink Set 생성 및 반환
	 */
	public Set<Sinks.Many<String>> getOrCreate(String roomId) {
		return sinkMap.computeIfAbsent(roomId, key -> ConcurrentHashMap.newKeySet());
	}
}
