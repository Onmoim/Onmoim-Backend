package com.onmoim.server.group.dto.request;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;


// 모임(Group) 관련 요청의 제약 조건을 정의한 상수 클래스입니다.
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GroupRequestConstraints {
	// 모임 생성 시 최대 정원
	public static final int CREATE_MAX_CAPACITY = 300;
	// 모임 생성 시 최소 정원
	public static final int CREATE_MIN_CAPACITY = 5;
}
