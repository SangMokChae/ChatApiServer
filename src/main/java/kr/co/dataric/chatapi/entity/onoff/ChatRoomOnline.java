package kr.co.dataric.chatapi.entity.onoff;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "chat_room_online")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatRoomOnline {
	
	@Id
	private String roomId;
	
	private List<String> onlineUsers;
	
}
