package kr.co.dataric.common.dto;

import kr.co.dataric.common.entity.ChatRoom;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomRedisDto {
	private String roomId;
	private String roomName;
	private String lastMessage;
	private LocalDateTime lastMessageTime;
	private List<String> userIds;
	private String lastSender;
	private int readCount;
	
	// MongoDB에서 fallback할 때 사용
	public static ChatRoomRedisDto from(ChatRoom chatRoom) {
		return ChatRoomRedisDto.builder()
			.roomId(chatRoom.getRoomId())
			.roomName(chatRoom.getRoomName())
			.lastMessage(chatRoom.getLastMessage())
			.lastMessageTime(chatRoom.getLastMessageTime())
			.build();
	}
	
	public String getRedisKey() {
		return "chat_room:" + this.roomId + ":" + lastSender + ":" + readCount;
	}
	
}