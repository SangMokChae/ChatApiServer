package kr.co.dataric.common.entity.friends;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum FriendStatus {
	PENDING,    // 친구 요청 보낸 상태
	ACCEPTED    // 친구 관계 수락 완료 상태
}