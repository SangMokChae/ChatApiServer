package kr.co.dataric.common.entity;

import kr.co.dataric.common.entity.friends.FriendStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("friends")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Friends {
	
	@Id
	private Long id;
	
	@Column("user_id")
	private String userId;
	
	@Column("friend_id")
	private String friendId;
	
	@Column("friend_nickname")
	private String friendNickname;
	
	@Column("friend_status")
	private FriendStatus status; // Accepted, pending
	
	@Builder.Default
	@Column("created_at")
	private LocalDateTime createdAt = LocalDateTime.now();
	
}
