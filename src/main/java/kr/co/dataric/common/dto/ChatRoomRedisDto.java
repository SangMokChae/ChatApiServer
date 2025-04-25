package kr.co.dataric.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import kr.co.dataric.common.entity.ChatRoom;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.redis.core.RedisHash;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomRedisDto {
	private String roomId;
	private String roomName;
	private String lastMessage;
	private LocalDateTime lastMessageTime;
	private List<String> participants; // ✅ 각 유저의 unread 계산에 필요
	private String lastSender;
	private int readCount;
	private Map<String, Long> readCountMap; // ✅ 유저별 readCount 저장용
	
}