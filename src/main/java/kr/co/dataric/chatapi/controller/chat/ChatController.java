package kr.co.dataric.chatapi.controller.chat;

import kr.co.dataric.chatapi.service.ChatService;
import kr.co.dataric.chatapi.service.impl.ChatRoomOnlineService;
import kr.co.dataric.common.entity.ChatMessage;
import kr.co.dataric.common.jwt.provider.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatController {
	
	private final ChatService chatService;
	private final JwtProvider jwtProvider;
	private final ChatRoomOnlineService chatRoomOnlineService;
	
	@GetMapping("/history")
	public Flux<ChatMessage> getChatHistory(
		@RequestParam String roomId,
		@RequestParam int offset,
		@RequestParam(defaultValue = "30") int limit
	) {
		return chatService.getMessagesByRoom(roomId, offset, limit);
	}

}
