package kr.co.dataric.chatapi.dto.request.onoff;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OfflineRequestDto {
	
	private String roomId;
	private String userId;
	
}
