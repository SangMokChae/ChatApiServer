package kr.co.dataric.chatapi.dto.request.view;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ViewRequestDto {
	
	private String roomId;
	private String inUserIds;
	
}
