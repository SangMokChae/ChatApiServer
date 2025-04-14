package kr.co.dataric.chatapi.service;

import kr.co.dataric.common.entity.ChatMessage;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ChatService {
	Flux<ChatMessage> getMessagesByRoom(String roomId, int offset, int size);
	Mono<Void> saveChatMessage(ChatMessage message);
}
