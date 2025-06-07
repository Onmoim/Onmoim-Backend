package com.onmoim.server.meeting.service;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;
import com.onmoim.server.common.s3.dto.FileUploadResponseDto;
import com.onmoim.server.common.s3.service.FileStorageService;
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
 * 락 전략: 모든 일정 타입에 Named Lock 사용
 * - 빠른 타임아웃 (3초)으로 사용자 경험 개선
 * - Group과 동일한 락 전략으로 일관성 확보
 * - 서버 리소스 효율적 사용
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
	private final FileStorageService fileStorageService;

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
	 * 일정 참석 신청 (Named Lock 통일)
	 * - 모든 일정 타입에 동일한 3초 타임아웃 적용
	 * - 빠른 실패로 사용자 경험 개선
	 */
	@Transactional
	public void joinMeeting(Long meetingId) {
		Long userId = getCurrentUserId();
		User user = userQueryService.findById(userId);

		joinMeetingWithNamedLock(meetingId, userId, user);
	}

	/**
	 * 일정 참석 신청 (Named Lock)
	 */
	@Transactional
	public void joinMeetingWithNamedLock(Long meetingId, Long userId, User user) {
		String lockKey = "meeting_" + meetingId;

		// 조기 검증 (락 획득 전 빠른 실패)
		validateEarlyChecks(meetingId, userId);

		try {
			// Named Lock 획득 (3초 타임아웃)
			Integer lockResult = meetingRepository.getLock(lockKey, 3);
			if (lockResult == null || lockResult <= 0) {
				log.warn("일정 {} 참석 신청 중 Named Lock timeout 발생", meetingId);
				throw new CustomException(ErrorCode.MEETING_LOCK_TIMEOUT);
			}

			// 일정 조회 (락 보호 하에)
			Meeting meeting = meetingQueryService.getById(meetingId);

			// 최종 검증 및 참석 처리
			processJoinMeeting(meeting, user, userId, meetingId);

		} finally {
			// Named Lock 안전 해제
			try {
				meetingRepository.releaseLock(lockKey);
			} catch (Exception e) {
				log.error("Named Lock 해제 실패: {} - {}", lockKey, e.getMessage());
				// 락 해제 실패는 타임아웃으로 자동 해제되므로 시스템 장애로 이어지지 않음
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
	private void processJoinMeeting(Meeting meeting, User user, Long userId, Long meetingId) {
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

		log.info("사용자 {}가 일정 {}에 참석 신청했습니다. (Named Lock, 참석: {}/{})",
			userId, meetingId, meeting.getJoinCount(), meeting.getCapacity());
	}

	/**
	 * 일정 참석 취소 (Named Lock 사용)
	 */
	@Transactional
	public void leaveMeeting(Long meetingId) {
		Long userId = getCurrentUserId();
		String lockKey = "meeting_" + meetingId;

		try {
			// Named Lock 획득
			Integer lockResult = meetingRepository.getLock(lockKey, 3);
			if (lockResult == null || lockResult <= 0) {
				log.warn("일정 {} 참석 취소 중 Named Lock timeout 발생", meetingId);
				throw new CustomException(ErrorCode.MEETING_LOCK_TIMEOUT);
			}

			// 일정 조회
			Meeting meeting = meetingQueryService.getById(meetingId);

			// 일정 상태 검증
			validateMeetingForLeave(meeting);

			// 참석 여부 검증
			UserMeeting userMeeting = userMeetingRepository.findByMeetingIdAndUserId(meetingId, userId)
				.orElseThrow(() -> new CustomException(ErrorCode.MEETING_NOT_JOINED));

			// 참석 취소 처리
			meeting.leave();
			userMeetingRepository.delete(userMeeting);

			// 자동 삭제 로직: 남은 참석자가 1명 이하면 일정 삭제
			if (meeting.getJoinCount() <= 1) {
				deleteMeetingWithCleanup(meeting);
				log.info("일정 {} 자동 삭제 완료 (참석자 {}명 이하)", meetingId, meeting.getJoinCount());
			} else {
				log.info("사용자 {}가 일정 {}에서 참석 취소했습니다. (Named Lock, 참석: {}/{})",
					userId, meetingId, meeting.getJoinCount(), meeting.getCapacity());
			}

		} finally {
			try {
				meetingRepository.releaseLock(lockKey);
			} catch (Exception e) {
				log.error("Named Lock 해제 실패: {} - {}", lockKey, e.getMessage());
			}
		}
	}

	/**
	 * 일정 수정 (Named Lock 사용)
	 */
	@Transactional
	public void updateMeeting(Long meetingId, MeetingUpdateRequestDto request) {
		String lockKey = "meeting_" + meetingId;

		try {
			// Named Lock 획득
			Integer lockResult = meetingRepository.getLock(lockKey, 3);
			if (lockResult == null || lockResult <= 0) {
				throw new CustomException(ErrorCode.MEETING_LOCK_TIMEOUT);
			}

			// 일정 조회
			Meeting meeting = meetingQueryService.getById(meetingId);

			// 권한 검증 (모든 일정 타입에서 모임장만 수정 가능)
			validateOwnerPermission(meeting);

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

		} finally {
			try {
				meetingRepository.releaseLock(lockKey);
			} catch (Exception e) {
				log.error("Named Lock 해제 실패: {} - {}", lockKey, e.getMessage());
			}
		}
	}

	/**
	 * 일정 삭제 (Named Lock 사용)
	 * - 모든 일정 타입에서 모임장만 삭제 가능
	 */
	@Transactional
	public void deleteMeeting(Long meetingId) {
		String lockKey = "meeting_" + meetingId;

		try {
			// Named Lock 획득
			Integer lockResult = meetingRepository.getLock(lockKey, 3);
			if (lockResult == null || lockResult <= 0) {
				throw new CustomException(ErrorCode.MEETING_LOCK_TIMEOUT);
			}

			// 일정 조회
			Meeting meeting = meetingQueryService.getById(meetingId);

			// 권한 검증 (모든 일정 타입에서 모임장만 삭제 가능)
			validateOwnerPermission(meeting);

			// 일정 삭제 처리
			deleteMeetingWithCleanup(meeting);

			log.info("일정 {} 삭제 완료 (모임장 권한)", meetingId);

		} finally {
			try {
				meetingRepository.releaseLock(lockKey);
			} catch (Exception e) {
				log.error("Named Lock 해제 실패: {} - {}", lockKey, e.getMessage());
			}
		}
	}

	/**
	 * 일정 삭제 및 정리 작업
	 */
	private void deleteMeetingWithCleanup(Meeting meeting) {
		// 관련된 UserMeeting 데이터 삭제
		userMeetingRepository.deleteByMeetingId(meeting.getId());

		// 이미지가 있다면 S3에서 삭제
		if (meeting.getImgUrl() != null) {
			try {
				fileStorageService.deleteFile(meeting.getImgUrl());
				log.info("일정 {} 이미지 삭제 완료: {}", meeting.getId(), meeting.getImgUrl());
			} catch (Exception e) {
				log.warn("일정 {} 이미지 삭제 실패: {}", meeting.getId(), e.getMessage());
			}
		}

		// 일정 소프트 삭제
		meeting.softDelete();
	}

	/**
	 * 일정 이미지 업로드
	 * - 정기모임: 모임장만 가능
	 * - 번개모임: 모임장 또는 주최자 가능
	 */
	@Transactional
	public FileUploadResponseDto uploadMeetingImage(Long meetingId, MultipartFile file) {
		Long userId = getCurrentUserId();
		Meeting meeting = meetingQueryService.getById(meetingId);

		// 권한 검증 (이미지 업로드는 생성 권한과 동일)
		validateImageUploadPermission(meeting, userId);

		// 기존 이미지가 있다면 삭제
		if (meeting.getImgUrl() != null) {
			try {
				fileStorageService.deleteFile(meeting.getImgUrl());
			} catch (Exception e) {
				log.warn("기존 이미지 삭제 실패: {}", e.getMessage());
			}
		}

		// 새 이미지 업로드
		FileUploadResponseDto response = fileStorageService.uploadFile(file, "meetings");

		// Meeting 엔티티 업데이트
		meeting.updateImage(response.getFileUrl());

		log.info("일정 {} 이미지 업로드 성공: {}", meetingId, response.getFileUrl());

		return response;
	}

	/**
	 * 일정 이미지 삭제
	 * - 모든 일정 타입에서 모임장만 가능 (관리 권한)
	 */
	@Transactional
	public void deleteMeetingImage(Long meetingId) {
		Long userId = getCurrentUserId();
		Meeting meeting = meetingQueryService.getById(meetingId);

		// 권한 검증 (이미지 삭제는 모임장만 가능)
		validateOwnerPermission(meeting);

		if (meeting.getImgUrl() == null) {
			throw new CustomException(ErrorCode.INVALID_USER);
		}

		// S3에서 이미지 삭제
		try {
			fileStorageService.deleteFile(meeting.getImgUrl());
		} catch (Exception e) {
			log.error("S3 이미지 삭제 실패: {}", e.getMessage());
			throw new CustomException(ErrorCode.FILE_DELETE_FAILED);
		}

		// Meeting 엔티티에서 이미지 URL 제거
		meeting.updateImage(null);

		log.info("일정 {} 이미지 삭제 성공", meetingId);
	}

	/**
	 * 모임장 권한 검증 (수정/삭제/이미지 삭제 공통)
	 */
	private void validateOwnerPermission(Meeting meeting) {
		Long currentUserId = getCurrentUserId();
		meetingPermissionService.validateOwnerPermission(meeting.getGroupId(), currentUserId);
	}

	/**
	 * 이미지 업로드 권한 검증 (생성 권한과 동일)
	 * - 정기모임: 모임장만 가능
	 * - 번개모임: 모임장 또는 주최자 가능
	 */
	private void validateImageUploadPermission(Meeting meeting, Long userId) {
		if (meeting.getType() == MeetingType.REGULAR) {
			// 정기모임: 모임장만
			meetingPermissionService.validateOwnerPermission(meeting.getGroupId(), userId);
		} else {
			// 번개모임: 모임장 또는 주최자
			try {
				// 먼저 모임장 권한 확인
				meetingPermissionService.validateOwnerPermission(meeting.getGroupId(), userId);
			} catch (CustomException e) {
				// 모임장이 아니면 주최자(작성자) 권한 확인
				if (!meeting.getCreatorId().equals(userId)) {
					throw new CustomException(ErrorCode.GROUP_FORBIDDEN);
				}
				// 주최자인 경우 통과
			}
		}
	}

	// ===== 검증 메서드들 =====

	private void validateCreatePermission(Long groupId, Long userId, MeetingType type) {
		if (type == MeetingType.REGULAR) {
			meetingPermissionService.validateOwnerPermission(groupId, userId);
		} else {
			meetingPermissionService.validateJoinPermission(groupId, userId);
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

