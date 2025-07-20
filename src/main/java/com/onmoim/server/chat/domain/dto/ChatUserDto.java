package com.onmoim.server.chat.domain.dto;

import com.onmoim.server.user.entity.User;

import lombok.Data;

@Data
public class ChatUserDto {
	private Long id;
	private String username;
	private String profileImageUrl;
	private boolean isOwner;

	private ChatUserDto(Long id, String username, String profileImageUrl, boolean isOwner) {
		this.id = id;
		this.username = username;
		this.profileImageUrl = profileImageUrl;
		this.isOwner = isOwner;
	}

	public static ChatUserDto createSystem() {
		return new ChatUserDto(null, "SYSTEM", null, false);
	}

	public static ChatUserDto create(User user, boolean isOwner) {
		return new ChatUserDto(user.getId(), user.getName(), user.getProfileImgUrl(), isOwner);
	}
}
