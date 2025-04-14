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
	private String lastMessage;
	private LocalDateTime lastMessageTime;
	private List<String> userIds;  // 참여 유저 ID 목록
	
	public static ChatRoomRedisDto from(ChatRoom chatRoom) {
		return ChatRoomRedisDto.builder()
			.roomId(chatRoom.getRoomId())
			.lastMessage(chatRoom.getLastMessage())
			.lastMessageTime(chatRoom.getLastMessageTime())
			.build();
	}
	
}
