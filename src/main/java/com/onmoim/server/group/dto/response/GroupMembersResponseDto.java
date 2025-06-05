package com.onmoim.server.group.dto.response;

import com.onmoim.server.group.entity.GroupUser;
import com.onmoim.server.user.entity.User;

import io.swagger.v3.oas.annotations.media.Schema;

public record GroupMembersResponseDto(
	@Schema(description = "유저 ID")
	Long userId,
	@Schema(description = "유저 이름")
	String username,
	@Schema(description = "유저 프로필")
 	String profileImageUrl,
	@Schema(description = "모임장 또는 멤버")
	 String role
) {
	public static GroupMembersResponseDto of(GroupUser groupUser) {
		User user = groupUser.getUser();
		return new GroupMembersResponseDto(
			user.getId(),
			user.getName(),
			user.getProfileImgUrl(),
			groupUser.getStatus().getDescription()
		);
	}
}
