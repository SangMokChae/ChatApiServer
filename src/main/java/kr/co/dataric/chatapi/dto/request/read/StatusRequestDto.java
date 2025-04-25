package kr.co.dataric.chatapi.dto.request.read;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StatusRequestDto {
	
	private String roomId;
	private String userId;
	private String lastRead;
	private String msgId;
	private String timestamp;
	private String status;
	private List<String> participants;
	
}
