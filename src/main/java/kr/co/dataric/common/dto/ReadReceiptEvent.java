package kr.co.dataric.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReadReceiptEvent {
	private String msgId;
	private String userId;
	private String roomId;
	private LocalDateTime readAt;
}
