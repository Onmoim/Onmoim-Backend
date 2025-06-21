package com.onmoim.server.group.dto;

import com.onmoim.server.group.entity.GroupUser;
import com.onmoim.server.user.entity.User;

public record GroupMember(
	Long memberId,
	String username,
	String profileImageUrl,
	String role
)
{
	public static GroupMember of(GroupUser groupUser) {
		User user = groupUser.getUser();
		return new GroupMember(
			user.getId(),
			user.getName(),
			user.getProfileImgUrl(),
			groupUser.getStatus().getDescription()
		);
	}
}
