package com.onmoim.server.group.dto.request;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 모임(Group) 관련 요청의 제약 조건을 정의한 상수 클래스입니다.
 * 엔티티 내부에서 요청 검증을 처리에 대한 의견입니다!
 * 1. 요청 필드마다 서로 다른 응답 형태(FieldErrorResponse, Custom 등)를 필요로 하게 됩니다.
 * 2. 검증 책임이 엔티티로 넘어가면서, 역할이 모호해지고 응집도가 떨어집니다.
 * yaml 같은 외부 파일로 관리
 * 1. "DTO"는 빈으로 등록되지 않기 때문에, 외부 설정 값을 주입받는 데 한계가 있었습니다..!
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GroupRequestConstraints {
	// 모임 생성 시 최대 정원
	public static final int CREATE_MAX_CAPACITY = 300;
	// 모임 생성 시 최소 정원
	public static final int CREATE_MIN_CAPACITY = 5;
}
