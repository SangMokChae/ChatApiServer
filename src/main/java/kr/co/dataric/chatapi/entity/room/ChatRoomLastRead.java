package kr.co.dataric.chatapi.entity.room;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

@Document(collection = "chat_room_last_read")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatRoomLastRead {
	
	@Id
	private String id;
	
	private String roomId;
	
	private Map<String, String> lastReadMap; // userId -. lastMessageId
	
}
