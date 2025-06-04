package com.onmoim.server.meeting.service;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;
import com.onmoim.server.group.entity.GroupUser;
import com.onmoim.server.group.entity.Status;
import com.onmoim.server.group.service.GroupQueryService;
import com.onmoim.server.group.service.GroupUserQueryService;
import com.onmoim.server.meeting.entity.Meeting;
import com.onmoim.server.meeting.entity.MeetingType;
import com.onmoim.server.meeting.entity.UserMeeting;
import com.onmoim.server.meeting.repository.UserMeetingRepository;
import com.onmoim.server.security.CustomUserDetails;
import com.onmoim.server.user.entity.User;
import com.onmoim.server.user.service.UserQueryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingService {

	private final MeetingQueryService meetingQueryService;
	private final UserQueryService userQueryService;
	private final GroupQueryService groupQueryService;
	private final GroupUserQueryService groupUserQueryService;
	private final UserMeetingRepository userMeetingRepository;

	/**
	 * 일정 참석 신청
	 */
	@Transactional
	public void joinMeeting(Long meetingId) {
		Long userId = getCurrentUserId();
		User user = userQueryService.findById(userId);
		
		// 락을 적용한 일정 조회
		Meeting meeting = meetingQueryService.getByIdWithLock(meetingId);
		
		// 이미 참석했는지 확인
		if (userMeetingRepository.existsByMeetingIdAndUserId(meetingId, userId)) {
			throw new CustomException(ErrorCode.GROUP_ALREADY_JOINED);
		}
		
		// 모임 멤버인지 확인
		validateGroupMember(meeting.getGroupId(), userId);
		
		// 정원 확인 및 참석 처리
		meeting.join();
		
		// 참석 정보 저장
		UserMeeting userMeeting = UserMeeting.create(meeting, user);
		userMeetingRepository.save(userMeeting);
	}

	/**
	 * 일정 참석 취소
	 */
	@Transactional
	public void leaveMeeting(Long meetingId) {
		Long userId = getCurrentUserId();
		
		// 락을 적용한 일정 조회
		Meeting meeting = meetingQueryService.getByIdWithLock(meetingId);
		
		// 참석 정보 확인
		UserMeeting userMeeting = userMeetingRepository.findByMeetingIdAndUserId(meetingId, userId)
			.orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
		
		// 참석 취소 처리
		meeting.leave();
		userMeetingRepository.delete(userMeeting);
	}

	/**
	 * 일정 생성 권한 확인
	 */
	public void validateCreatePermission(Long groupId, MeetingType type) {
		Long userId = getCurrentUserId();
		GroupUser groupUser = groupUserQueryService.findById(groupId, userId)
			.orElseThrow(() -> new CustomException(ErrorCode.NOT_GROUP_MEMBER));
		
		if (type == MeetingType.REGULAR) {
			// 정기모임은 모임장만 생성 가능
			if (!groupUser.isOwner()) {
				throw new CustomException(ErrorCode.GROUP_FORBIDDEN);
			}
		} else {
			// 번개모임은 모임원 이상 생성 가능
			if (!groupUser.isJoined()) {
				throw new CustomException(ErrorCode.NOT_GROUP_MEMBER);
			}
		}
	}

	/**
	 * 일정 수정 권한 확인
	 */
	public void validateEditPermission(Meeting meeting) {
		Long userId = getCurrentUserId();
		
		// 24시간 전까지만 수정 가능
		if (!meeting.canEdit()) {
			throw new CustomException(ErrorCode.GROUP_FORBIDDEN);
		}
		
		if (meeting.getType() == MeetingType.REGULAR) {
			// 정기모임은 모임장만 수정 가능
			GroupUser groupUser = groupUserQueryService.findById(meeting.getGroupId(), userId)
				.orElseThrow(() -> new CustomException(ErrorCode.NOT_GROUP_MEMBER));
			if (!groupUser.isOwner()) {
				throw new CustomException(ErrorCode.GROUP_FORBIDDEN);
			}
		} else {
			// 번개모임은 작성자만 수정 가능
			if (!meeting.getCreatorId().equals(userId)) {
				throw new CustomException(ErrorCode.GROUP_FORBIDDEN);
			}
		}
	}

	/**
	 * 모임 멤버인지 확인
	 */
	private void validateGroupMember(Long groupId, Long userId) {
		GroupUser groupUser = groupUserQueryService.findById(groupId, userId)
			.orElseThrow(() -> new CustomException(ErrorCode.NOT_GROUP_MEMBER));
		if (!groupUser.isJoined()) {
			throw new CustomException(ErrorCode.NOT_GROUP_MEMBER);
		}
	}

	/**
	 * 현재 사용자 ID 조회
	 */
	private Long getCurrentUserId() {
		CustomUserDetails principal = (CustomUserDetails) SecurityContextHolder.getContextHolderStrategy()
			.getContext()
			.getAuthentication()
			.getPrincipal();
		return principal.getUserId();
	}
} 