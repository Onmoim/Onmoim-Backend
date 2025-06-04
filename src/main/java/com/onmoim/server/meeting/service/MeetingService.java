package com.onmoim.server.meeting.service;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;
import com.onmoim.server.meeting.dto.request.MeetingCreateRequestDto;
import com.onmoim.server.meeting.dto.request.MeetingUpdateRequestDto;
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

import java.time.LocalDateTime;

/**
 * 일정 관리 서비스
 * 
 * 락 전략:
 * - 정기모임(REGULAR): 비관적 락 (300명 동시 참석 가능으로 인한 높은 충돌)
 * - 번개모임(FLASH): 네임드 락 (소규모 특성으로 상대적 저충돌)
 */
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
	public Long createMeeting(Long groupId, MeetingCreateRequestDto request) {
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
	 * 일정 참석 신청 (타입별 최적화된 락 전략)
	 * - 정기모임: 비관적 락 (300명 동시 신청 대응)
	 * - 번개모임: 네임드 락 (가벼운 처리)
	 */
	@Transactional
	public void joinMeeting(Long meetingId) {
		Long userId = getCurrentUserId();
		User user = userQueryService.findById(userId);
		
		// 일정 타입 확인
		Meeting meeting = meetingQueryService.getById(meetingId);
		
		if (meeting.getType() == MeetingType.REGULAR) {
			// 정기모임: 300명 동시 신청 가능 → 비관적 락 필수
			joinMeetingWithPessimisticLock(meetingId, userId, user);
		} else {
			// 번개모임: 소규모 특성 → 네임드 락으로 가볍게
			joinMeetingWithNamedLock(meetingId, userId, user);
		}
	}

	/**
	 * 일정 참석 신청 (비관적 락 - 정기모임용)
	 */
	@Transactional
	public void joinMeetingWithPessimisticLock(Long meetingId, Long userId, User user) {
		// 조기 검증 (불필요한 락 획득 방지)
		validateEarlyChecks(meetingId, userId);
		
		try {
			// 비관적 락으로 일정 조회
			Meeting meeting = meetingQueryService.getByIdWithLock(meetingId);
			
			// 최종 검증 및 참석 처리
			processJoinMeeting(meeting, user, userId, meetingId, "비관적락");
			
		} catch (org.springframework.dao.CannotAcquireLockException | 
		         jakarta.persistence.PessimisticLockException e) {
			log.warn("일정 {} 참석 신청 중 Lock timeout 발생: {}", meetingId, e.getMessage());
			throw new CustomException(ErrorCode.MEETING_LOCK_TIMEOUT);
		}
	}

	/**
	 * 일정 참석 신청 (네임드 락 - 번개모임용)
	 */
	@Transactional
	public void joinMeetingWithNamedLock(Long meetingId, Long userId, User user) {
		String lockKey = "meeting_" + meetingId;
		
		try {
			// 네임드 락 획득
			Integer lockResult = meetingRepository.getLock(lockKey, 3);
			if (lockResult == null || lockResult <= 0) {
				throw new CustomException(ErrorCode.MEETING_LOCK_TIMEOUT);
			}
			
			// 일정 조회 (락 없이)
			Meeting meeting = meetingQueryService.getById(meetingId);
			
			// 검증 및 참석 처리
			processJoinMeeting(meeting, user, userId, meetingId, "네임드락");
			
		} finally {
			// 네임드 락 해제
			try {
				meetingRepository.releaseLock(lockKey);
			} catch (Exception e) {
				log.error("네임드 락 해제 실패: {}", lockKey, e);
			}
		}
	}

	/**
	 * 조기 검증 (락 획득 전 빠른 실패)
	 */
	private void validateEarlyChecks(Long meetingId, Long userId) {
		Meeting quickCheck = meetingQueryService.getById(meetingId);
		
		// 명백한 실패 케이스 조기 차단
		if (quickCheck.getStatus() == MeetingStatus.CLOSED || quickCheck.isStarted()) {
			throw new CustomException(ErrorCode.MEETING_ALREADY_CLOSED);
		}
		
		// 중복 참석 조기 차단
		if (userMeetingRepository.existsByMeetingIdAndUserId(meetingId, userId)) {
			throw new CustomException(ErrorCode.MEETING_ALREADY_JOINED);
		}
	}

	/**
	 * 공통 참석 처리 로직
	 */
	private void processJoinMeeting(Meeting meeting, User user, Long userId, Long meetingId, String lockType) {
		// 일정 상태 검증
		validateMeetingForJoin(meeting);
		
		// 정원 검증
		if (meeting.getJoinCount() >= meeting.getCapacity()) {
			throw new CustomException(ErrorCode.MEETING_CAPACITY_EXCEEDED);
		}
		
		// 참석 처리
		meeting.join();
		UserMeeting userMeeting = UserMeeting.create(meeting, user);
		userMeetingRepository.save(userMeeting);
		
		log.info("사용자 {}가 일정 {}에 참석 신청했습니다. ({}, 참석: {}/{})", 
			userId, meetingId, lockType, meeting.getJoinCount(), meeting.getCapacity());
	}

	/**
	 * 일정 참석 취소  
	 */
	@Transactional
	public void leaveMeeting(Long meetingId) {
		Long userId = getCurrentUserId();
		
		try {
			// 비관적 락으로 안전하게 처리 (참석 취소는 타입 무관하게 동일)
			Meeting meeting = meetingQueryService.getByIdWithLock(meetingId);
			
			// 일정 상태 검증
			validateMeetingForLeave(meeting);
			
			// 참석 여부 검증
			UserMeeting userMeeting = userMeetingRepository.findByMeetingIdAndUserId(meetingId, userId)
				.orElseThrow(() -> new CustomException(ErrorCode.MEETING_NOT_JOINED));
			
			// 참석 취소 처리
			meeting.leave();
			userMeetingRepository.delete(userMeeting);
			
			log.info("사용자 {}가 일정 {}에서 참석 취소했습니다. (참석: {}/{})", 
				userId, meetingId, meeting.getJoinCount(), meeting.getCapacity());
				
		} catch (org.springframework.dao.CannotAcquireLockException | 
		         jakarta.persistence.PessimisticLockException e) {
			log.warn("일정 {} 참석 취소 중 Lock timeout 발생: {}", meetingId, e.getMessage());
			throw new CustomException(ErrorCode.MEETING_LOCK_TIMEOUT);
		}
	}

	/**
	 * 일정 수정
	 */
	@Transactional
	public void updateMeeting(Long meetingId, MeetingUpdateRequestDto request) {
		// Lock 적용하여 조회
		Meeting meeting = meetingQueryService.getByIdWithLock(meetingId);
		
		// 권한 검증
		validateUpdatePermission(meeting);
		
		// 시간 제약 검증 (24시간 전)
		validateUpdateTime(meeting);
		
		// 정원 변경 검증
		validateCapacityChange(meeting, request.getCapacity());
		
		// 일정 정보 업데이트
		meeting.update(
			request.getTitle(),
			request.getStartAt(),
			request.getPlaceName(),
			request.getGeoPoint(),
			request.getCapacity().intValue(),
			request.getCost()
		);
		
		// 상태 재계산
		updateMeetingStatus(meeting);
	}

	// ===== 검증 메서드들 =====

	private void validateCreatePermission(Long groupId, Long userId, MeetingType type) {
		if (type == MeetingType.REGULAR) {
			meetingPermissionService.validateOwnerPermission(groupId, userId);
		} else {
			meetingPermissionService.validateJoinPermission(groupId, userId);
		}
	}

	private void validateUpdatePermission(Meeting meeting) {
		Long currentUserId = getCurrentUserId();
		
		if (meeting.getType() == MeetingType.REGULAR) {
			meetingPermissionService.validateOwnerPermission(meeting.getGroupId(), currentUserId);
		} else {
			if (!meeting.getCreatorId().equals(currentUserId)) {
				throw new CustomException(ErrorCode.MEETING_UPDATE_FORBIDDEN);
			}
		}
	}
	
	private void validateUpdateTime(Meeting meeting) {
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime cutoffTime = meeting.getStartAt().minusHours(24);
		
		if (now.isAfter(cutoffTime)) {
			throw new CustomException(ErrorCode.MEETING_UPDATE_TIME_EXCEEDED);
		}
	}
	
	private void validateCapacityChange(Meeting meeting, int newCapacity) {
		if (newCapacity < meeting.getJoinCount()) {
			throw new CustomException(ErrorCode.MEETING_CAPACITY_CANNOT_REDUCE);
		}
	}
	
	private void updateMeetingStatus(Meeting meeting) {
		if (meeting.getJoinCount() >= meeting.getCapacity()) {
			meeting.updateStatus(MeetingStatus.FULL);
		} else if (meeting.getStatus() == MeetingStatus.FULL) {
			meeting.updateStatus(MeetingStatus.OPEN);
		}
	}

	private void validateMeetingForJoin(Meeting meeting) {
		if (meeting.getStatus() == MeetingStatus.CLOSED) {
			throw new CustomException(ErrorCode.MEETING_ALREADY_CLOSED);
		}
		
		if (meeting.isStarted()) {
			throw new CustomException(ErrorCode.MEETING_ALREADY_CLOSED);
		}
	}
	
	private void validateMeetingForLeave(Meeting meeting) {
		if (meeting.getStatus() == MeetingStatus.CLOSED) {
			throw new CustomException(ErrorCode.MEETING_ALREADY_CLOSED);
		}
		
		if (meeting.isStarted()) {
			throw new CustomException(ErrorCode.MEETING_ALREADY_CLOSED);
		}
	}

	private Long getCurrentUserId() {
		CustomUserDetails principal = (CustomUserDetails) SecurityContextHolder.getContextHolderStrategy()
			.getContext()
			.getAuthentication()
			.getPrincipal();
		return principal.getUserId();
	}
}

