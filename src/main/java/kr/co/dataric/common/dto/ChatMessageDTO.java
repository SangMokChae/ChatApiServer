package kr.co.dataric.common.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ChatMessageDTO {
	
	private String roomId;            // 채팅방 ID
	private String sender;            // 보낸 사람
	private String message;           // 메시지 본문
	private LocalDateTime timestamp;  // 보낸 시각
	private List<String> participants; // 참여자 목록

}
