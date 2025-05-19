package com.onmoim.server.group.dto.response;

import com.onmoim.server.group.entity.GroupUser;
import com.onmoim.server.user.entity.User;

import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
public class GroupMembersResponseDto {
	private Long userId;
	private String username;
	private String profileImageUrl;
	private String role;

	public GroupMembersResponseDto(GroupUser groupUser) {
		User user = groupUser.getUser();
		this.userId = user.getId();
		this.username = user.getName();
		this.profileImageUrl = user.getProfileImgUrl();
		this.role = groupUser.getStatus().getDescription();
	}
}
