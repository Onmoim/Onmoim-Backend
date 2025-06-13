package com.onmoim.server.group.entity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum Status {
	PENDING("모임 임시 상태"),
	OWNER("모임장"),
	MEMBER("회원"),
	BOOKMARK("북마크"),
	BAN("차단"),
	DELETED("삭제");
	private final String description;
}
