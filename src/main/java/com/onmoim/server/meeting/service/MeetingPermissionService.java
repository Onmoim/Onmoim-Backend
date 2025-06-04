package com.onmoim.server.meeting.service;

import org.springframework.stereotype.Service;

import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;
import com.onmoim.server.group.entity.GroupUser;
import com.onmoim.server.group.service.GroupUserQueryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 일정 관련 권한 검증을 담당하는 도메인 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingPermissionService {

	private final GroupUserQueryService groupUserQueryService;

	/**
	 * 일정 참석 권한 확인
	 */
	public void validateJoinPermission(Long groupId, Long userId) {
		GroupUser groupUser = getGroupUser(groupId, userId);
		if (!groupUser.isJoined()) {
			throw new CustomException(ErrorCode.NOT_GROUP_MEMBER);
		}
	}

	/**
	 * 그룹 사용자 정보 조회
	 */
	private GroupUser getGroupUser(Long groupId, Long userId) {
		return groupUserQueryService.findById(groupId, userId)
			.orElseThrow(() -> new CustomException(ErrorCode.NOT_GROUP_MEMBER));
	}
} 