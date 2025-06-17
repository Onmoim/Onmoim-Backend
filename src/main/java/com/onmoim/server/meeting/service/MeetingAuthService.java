package com.onmoim.server.meeting.service;

import com.onmoim.server.group.implement.GroupUserQueryService;
import org.springframework.stereotype.Service;

import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;
import com.onmoim.server.group.entity.GroupUser;

import com.onmoim.server.meeting.entity.Meeting;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Meeting 도메인 권한 검증 서비스
 * - 모든 Meeting 관련 권한 검증을 담당
 * - 비즈니스 로직과 권한 검증 분리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingAuthService {

    private final GroupUserQueryService groupUserQueryService;

    /**
     * 일정 생성 권한 검증
     */
    public GroupUser validateCreatePermission(Long groupId, Long userId, Meeting meeting) {
        GroupUser groupUser = getGroupUser(groupId, userId);

        if (!meeting.canBeCreatedBy(groupUser)) {
            log.warn("일정 생성 권한 없음 - 그룹: {}, 사용자: {}, 권한: {}",
                groupId, userId, groupUser.getStatus());
            throw new CustomException(ErrorCode.GROUP_FORBIDDEN);
        }

        return groupUser;
    }

    /**
     * 일정 관리 권한 검증 (수정/삭제)
     */
    public GroupUser validateManagePermission(Meeting meeting, Long userId) {
        GroupUser groupUser = getGroupUser(meeting.getGroupId(), userId);

        if (!meeting.canBeManagedBy(groupUser)) {
            log.warn("일정 관리 권한 없음 - 일정: {}, 사용자: {}, 권한: {}",
                meeting.getId(), userId, groupUser.getStatus());
            throw new CustomException(ErrorCode.GROUP_FORBIDDEN);
        }

        return groupUser;
    }

    /**
     * 일정 이미지 업로드 권한 검증
     */
    public GroupUser validateImageUploadPermission(Meeting meeting, Long userId) {
        GroupUser groupUser = getGroupUser(meeting.getGroupId(), userId);

        if (!meeting.canUpdateImageBy(groupUser)) {
            log.warn("이미지 업로드 권한 없음 - 일정: {}, 사용자: {}, 권한: {}",
                meeting.getId(), userId, groupUser.getStatus());
            throw new CustomException(ErrorCode.GROUP_FORBIDDEN);
        }

        return groupUser;
    }

    /**
     * 일정 이미지 삭제 권한 검증
     */
    public GroupUser validateImageDeletePermission(Meeting meeting, Long userId) {
        GroupUser groupUser = getGroupUser(meeting.getGroupId(), userId);

        if (!meeting.canDeleteImageBy(groupUser)) {
            log.warn("이미지 삭제 권한 없음 - 일정: {}, 사용자: {}, 권한: {}",
                meeting.getId(), userId, groupUser.getStatus());
            throw new CustomException(ErrorCode.GROUP_FORBIDDEN);
        }

        return groupUser;
    }

    /**
     * 그룹 사용자 정보 조회
     */
    private GroupUser getGroupUser(Long groupId, Long userId) {
        return groupUserQueryService.findById(groupId, userId)
            .orElseThrow(() -> {
                log.warn("그룹 멤버 아님 - 그룹: {}, 사용자: {}", groupId, userId);
                return new CustomException(ErrorCode.NOT_GROUP_MEMBER);
            });
    }
}
