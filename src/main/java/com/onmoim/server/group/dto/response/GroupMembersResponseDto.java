package com.onmoim.server.group.dto.response;

import com.onmoim.server.group.dto.GroupMember;

import io.swagger.v3.oas.annotations.media.Schema;

public record GroupMembersResponseDto(
	@Schema(description = "유저 ID")
	Long memberId,
	@Schema(description = "유저 이름")
	String username,
	@Schema(description = "유저 프로필")
 	String profileImageUrl,
	@Schema(description = "모임장 또는 멤버")
	String role
) {
	public static GroupMembersResponseDto of(GroupMember groupMember) {
		return new GroupMembersResponseDto(
			groupMember.memberId(),
			groupMember.username(),
			groupMember.profileImageUrl(),
			groupMember.role()
		);
	}
}
