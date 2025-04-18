package kr.co.dataric.chatapi.config.sink;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class StatusSinkManager {
	private final Map<String, Set<Sinks.Many<String>>> sinkMap = new ConcurrentHashMap<>();
	
	public void register(String roomId, Sinks.Many<String> sink) {
		sinkMap.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(sink);
	}
	
	public void remove(String roomId, Sinks.Many<String> sink) {
		Set<Sinks.Many<String>> sinks = sinkMap.get(roomId);
		if (sinks != null) {
			sinks.remove(sink);
			if (sinks.isEmpty()) sinkMap.remove(roomId);
		}
	}
	
	public void emit(String roomId, String message) {
		Set<Sinks.Many<String>> sinks = sinkMap.get(roomId);
		if (sinks != null) {
			sinks.forEach(sink -> sink.tryEmitNext(message));
		}
	}
}
