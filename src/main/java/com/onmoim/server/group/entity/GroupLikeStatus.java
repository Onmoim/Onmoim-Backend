package com.onmoim.server.group.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum GroupLikeStatus {
	LIKE("좋아요"),
	PENDING("임시 상태"),
	NEW("생성 상태");
	private final String description;
}
