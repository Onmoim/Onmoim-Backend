package com.onmoim.server.meeting.service;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;
import com.onmoim.server.meeting.dto.request.MeetingCreateRequest;
import com.onmoim.server.meeting.entity.Meeting;
import com.onmoim.server.meeting.entity.MeetingStatus;
import com.onmoim.server.meeting.entity.MeetingType;
import com.onmoim.server.meeting.entity.UserMeeting;
import com.onmoim.server.meeting.repository.MeetingRepository;
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

	private final MeetingRepository meetingRepository;
	private final MeetingQueryService meetingQueryService;
	private final UserQueryService userQueryService;
	private final MeetingPermissionService meetingPermissionService;
	private final UserMeetingRepository userMeetingRepository;

	/**
	 * 일정 생성
	 */
	@Transactional
	public Long createMeeting(Long groupId, MeetingCreateRequest request) {
		Long userId = getCurrentUserId();
		User user = userQueryService.findById(userId);
		
		// 권한 검증 (정기모임=모임장, 번개모임=모임원)
		validateCreatePermission(groupId, userId, request.getType());
		
		// 일정 생성
		Meeting meeting = Meeting.meetingCreateBuilder()
			.groupId(groupId)
			.type(request.getType())
			.title(request.getTitle())
			.startAt(request.getStartAt())
			.placeName(request.getPlaceName())
			.geoPoint(request.getGeoPoint())
			.capacity(request.getCapacity())
			.cost(request.getCost())
			.creatorId(userId)
			.build();
		
		Meeting savedMeeting = meetingRepository.save(meeting);
		
		// 생성자는 자동으로 참석 처리
		UserMeeting userMeeting = UserMeeting.create(savedMeeting, user);
		userMeetingRepository.save(userMeeting);
		savedMeeting.join();
		
		log.info("사용자 {}가 모임 {}에 일정 {}을 생성했습니다.", userId, groupId, savedMeeting.getId());
		
		return savedMeeting.getId();
	}

	/**
	 * 일정 참석 신청
	 */
	@Transactional
	public void joinMeeting(Long meetingId) {
		Long userId = getCurrentUserId();
		User user = userQueryService.findById(userId);
		
		// 락을 적용한 일정 조회
		Meeting meeting = meetingQueryService.getByIdWithLock(meetingId);
		
		// 권한 검증
		meetingPermissionService.validateJoinPermission(meeting.getGroupId(), userId);
		
		// 중복 참석 확인
		validateNotAlreadyJoined(meetingId, userId);
		
		// 일정 상태 확인
		validateMeetingJoinable(meeting);
		
		// 정원 확인 및 참석 처리
		meeting.join();
		
		// 참석 정보 저장
		UserMeeting userMeeting = UserMeeting.create(meeting, user);
		userMeetingRepository.save(userMeeting);
		
		log.info("사용자 {}가 일정 {}에 참석 신청했습니다.", userId, meetingId);
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
		UserMeeting userMeeting = getUserMeeting(meetingId, userId);
		
		// 일정 상태 확인
		validateMeetingLeavable(meeting);
		
		// 참석 취소 처리
		meeting.leave();
		userMeetingRepository.delete(userMeeting);
		
		log.info("사용자 {}가 일정 {}에서 참석 취소했습니다.", userId, meetingId);
	}

	/**
	 * 일정 생성 권한 검증
	 */
	private void validateCreatePermission(Long groupId, Long userId, MeetingType type) {
		if (type == MeetingType.REGULAR) {
			// 정기모임은 모임장만 생성 가능
			meetingPermissionService.validateOwnerPermission(groupId, userId);
		} else {
			// 번개모임은 모임원이면 생성 가능
			meetingPermissionService.validateJoinPermission(groupId, userId);
		}
	}

	/**
	 * 중복 참석 확인
	 */
	private void validateNotAlreadyJoined(Long meetingId, Long userId) {
		if (userMeetingRepository.existsByMeetingIdAndUserId(meetingId, userId)) {
			throw new CustomException(ErrorCode.GROUP_ALREADY_JOINED);
		}
	}

	/**
	 * 일정 참석 가능 여부 확인
	 */
	private void validateMeetingJoinable(Meeting meeting) {
		if (meeting.getStatus() == MeetingStatus.CLOSED) {
			throw new CustomException(ErrorCode.GROUP_FORBIDDEN);
		}
		if (meeting.isStarted()) {
			throw new CustomException(ErrorCode.GROUP_FORBIDDEN);
		}
	}

	/**
	 * 일정 참석 취소 가능 여부 확인
	 */
	private void validateMeetingLeavable(Meeting meeting) {
		if (meeting.getStatus() == MeetingStatus.CLOSED) {
			throw new CustomException(ErrorCode.GROUP_FORBIDDEN);
		}
		if (meeting.isStarted()) {
			throw new CustomException(ErrorCode.GROUP_FORBIDDEN);
		}
	}

	/**
	 * 사용자 참석 정보 조회
	 */
	private UserMeeting getUserMeeting(Long meetingId, Long userId) {
		return userMeetingRepository.findByMeetingIdAndUserId(meetingId, userId)
			.orElseThrow(() -> new CustomException(ErrorCode.NOT_EXISTS_GROUP));
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