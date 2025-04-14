package kr.co.dataric.chatapi.entity.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Table("users")
public class User {
	
	@Id
	private Long id;
	private String username;
	private String password;
	private String role;
	private boolean enabled;
	private String email;
	private String phone;
	private boolean active;
	private LocalDateTime createdAt;
	private String nickname;
}
