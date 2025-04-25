package kr.co.dataric.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
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
public class ReadReceiptEvent {
	
	private String msgId;
	private String userId;
	private String roomId;
	
	private LocalDateTime timestamp;
	
	private String lastRead;
	private String status;
	
	private List<String> participants; // 🔥 읽음 처리 대상자 목록
}