package com.onmoim.server.group.dto.response;

import com.onmoim.server.group.entity.GroupUser;
import com.onmoim.server.user.entity.User;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
public class GroupMembersResponseDto {

	@Schema(description = "유저 ID")
	private Long userId;
	@Schema(description = "유저 이름")
	private String username;
	@Schema(description = "유저 프로필")
	private String profileImageUrl;
	@Schema(description = "모임장 또는 멤버")
	private String role;

	public GroupMembersResponseDto(GroupUser groupUser) {
		User user = groupUser.getUser();
		this.userId = user.getId();
		this.username = user.getName();
		this.profileImageUrl = user.getProfileImgUrl();
		this.role = groupUser.getStatus().getDescription();
	}
}
