package kr.co.dataric.chatapi.dto.request.login;

import lombok.Data;

@Data
public class LoginRequest {
	private String userId;
	private String password;
}
