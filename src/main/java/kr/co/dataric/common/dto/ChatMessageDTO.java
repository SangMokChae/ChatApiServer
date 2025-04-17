package kr.co.dataric.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatMessageDTO {
	
	@JsonProperty("msgId")
	private String id; 								// 채팅 ID
	private String roomId;            // 채팅방 ID
	private String sender;            // 보낸 사람
	private String message;           // 메시지 본문
	private LocalDateTime timestamp;  // 보낸 시각
	private List<String> participants; // 참여자 목록
}
