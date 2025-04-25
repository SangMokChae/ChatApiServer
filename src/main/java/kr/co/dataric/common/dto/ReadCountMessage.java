package kr.co.dataric.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadCountMessage {
	private String roomId;             // 채팅방 ID
	private String userId;						// 사용중인 USER ID
	private String msgId;             // 메시지 고유 ID
	private int readCount;            // 읽지 않은 인원 수 or 읽음 대상 수
	private String lastMessage;       // (선택) 마지막 메시지 (Sink 전파용)
	private LocalDateTime lastMessageTime;   // (선택) 마지막 메시지 시간 (ISO string or yyyy-MM-dd HH:mm:ss 등)
	private String sender; // 필수
}
