package com.onmoim.server.post.entity;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum GroupPostType {
    ALL("전체"),
    NOTICE("공지"),
    INTRODUCTION("가입인사"),
    REVIEW("모임 후기"),
    FREE("자유 게시판");
	private final String description;
}
