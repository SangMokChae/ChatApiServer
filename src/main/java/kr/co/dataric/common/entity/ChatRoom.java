package kr.co.dataric.common.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "chat_room")
@Builder
public class ChatRoom {
	
	@Id
	private String id;
	
	@Indexed(unique = true)
	private String roomId;
	private List<String> participants; // userIds
	private String roomName;
	
	@Builder.Default
	private String roomType = "1";
	@Builder.Default
	private String roomTypeKey = null;
	
	private LocalDateTime createAt;
	private String lastMessage;
	private LocalDateTime lastMessageTime;
	
}