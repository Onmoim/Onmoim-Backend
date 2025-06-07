package com.onmoim.server.meeting.service;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;
import com.onmoim.server.common.s3.dto.FileUploadResponseDto;
import com.onmoim.server.common.s3.service.FileStorageService;
import com.onmoim.server.group.entity.GroupUser;
import com.onmoim.server.group.service.GroupUserQueryService;
import com.onmoim.server.meeting.dto.request.MeetingCreateRequestDto;
import com.onmoim.server.meeting.dto.request.MeetingUpdateRequestDto;
import com.onmoim.server.meeting.entity.Meeting;
import com.onmoim.server.meeting.entity.UserMeeting;
import com.onmoim.server.meeting.aop.MeetingLock;
import com.onmoim.server.meeting.repository.MeetingRepository;
import com.onmoim.server.meeting.repository.UserMeetingRepository;
import com.onmoim.server.security.CustomUserDetails;
import com.onmoim.server.user.entity.User;
import com.onmoim.server.user.service.UserQueryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


/**
 * 일정 관리 서비스
 *
 * 락 전략: AOP 기반 Named Lock
 * - @MeetingLock 어노테이션으로 동시성 제어
 * - 트랜잭션 시작 전 락 획득, 종료 후 락 해제
 * - 커넥션 추가 사용 없이 안전한 동시성 보장
 * - 타입별 타임아웃: 정기모임 1초, 번개모임 3초
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingService {

	private final MeetingRepository meetingRepository;
	private final MeetingQueryService meetingQueryService;
	private final UserQueryService userQueryService;
	private final GroupUserQueryService groupUserQueryService;
	private final UserMeetingRepository userMeetingRepository;
	private final FileStorageService fileStorageService;

	/**
	 * 일정 생성
	 */
	@Transactional
	public Long createMeeting(Long groupId, MeetingCreateRequestDto request) {
		Long userId = getCurrentUserId();
		User user = userQueryService.findById(userId);
		GroupUser groupUser = getGroupUser(groupId, userId);

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

		if (!meeting.canBeCreatedBy(groupUser)) {
			throw new CustomException(ErrorCode.GROUP_FORBIDDEN);
		}

		Meeting savedMeeting = meetingRepository.save(meeting);

		// 생성자는 자동으로 참석 처리
		UserMeeting userMeeting = UserMeeting.create(savedMeeting, user);
		userMeetingRepository.save(userMeeting);
		savedMeeting.join();

		log.info("사용자 {}가 모임 {}에 일정 {}을 생성했습니다.", userId, groupId, savedMeeting.getId());

		return savedMeeting.getId();
	}

	/**
	 * 일정 참석 신청 (AOP Named Lock 적용)
	 * - 트랜잭션 시작 전 락 획득, 종료 후 락 해제
	 * - 커넥션 추가 사용 없음
	 */
	@MeetingLock
	@Transactional
	public void joinMeeting(Long meetingId) {
		Long userId = getCurrentUserId();
		User user = userQueryService.findById(userId);

		// 조기 검증 (락 보호 하에서)
		Meeting meeting = meetingQueryService.getById(meetingId);
		if (!meeting.canJoin()) {
			throw new CustomException(ErrorCode.MEETING_ALREADY_CLOSED);
		}

		// 중복 참석 검증 (락 보호 하에서)
		if (userMeetingRepository.existsByMeetingIdAndUserId(meetingId, userId)) {
			throw new CustomException(ErrorCode.MEETING_ALREADY_JOINED);
		}

		// 비즈니스 로직 실행
		meeting.join();
		UserMeeting userMeeting = UserMeeting.create(meeting, user);
		userMeetingRepository.save(userMeeting);

		log.info("사용자 {}가 일정 {}에 참석 신청했습니다. (AOP Lock, 타입: {}, 참석: {}/{})",
			userId, meetingId, meeting.getType(), meeting.getJoinCount(), meeting.getCapacity());
	}

	/**
	 * 일정 참석 취소 (AOP Named Lock 적용)
	 * - 트랜잭션 시작 전 락 획득, 종료 후 락 해제
	 * - 커넥션 추가 사용 없음
	 */
	@MeetingLock
	@Transactional
	public void leaveMeeting(Long meetingId) {
		Long userId = getCurrentUserId();

		// 일정 조회 (락 보호 하에서)
		Meeting meeting = meetingQueryService.getById(meetingId);

		// 참석 여부 검증
		UserMeeting userMeeting = userMeetingRepository.findByMeetingIdAndUserId(meetingId, userId)
			.orElseThrow(() -> new CustomException(ErrorCode.MEETING_NOT_JOINED));

		meeting.leave();
		userMeetingRepository.delete(userMeeting);

		// 자동 삭제 로직: 도메인 규칙으로 확인
		if (meeting.shouldBeAutoDeleted()) {
			deleteMeetingWithCleanup(meeting);
			log.info("일정 {} 자동 삭제 완료 (참석자 {}명 이하)", meetingId, meeting.getJoinCount());
		} else {
			log.info("사용자 {}가 일정 {}에서 참석 취소했습니다. (AOP Lock, 타입: {}, 참석: {}/{})",
				userId, meetingId, meeting.getType(), meeting.getJoinCount(), meeting.getCapacity());
		}
	}

	/**
	 * 일정 수정 (트랜잭션만 사용 - 모임장 권한으로 동시성 이슈 없음)
	 */
	@Transactional
	public void updateMeeting(Long meetingId, MeetingUpdateRequestDto request) {
		// 일정 조회 및 권한 검증
		Meeting meeting = meetingQueryService.getById(meetingId);
		Long userId = getCurrentUserId();
		GroupUser groupUser = getGroupUser(meeting.getGroupId(), userId);

		if (!meeting.canBeManagedBy(groupUser)) {
			throw new CustomException(ErrorCode.GROUP_FORBIDDEN);
		}

		// 도메인 로직으로 일정 정보 업데이트
		meeting.updateMeetingInfo(
			request.getTitle(),
			request.getStartAt(),
			request.getPlaceName(),
			request.getGeoPoint(),
			request.getCapacity().intValue(),
			request.getCost()
		);

		log.info("일정 {} 수정 완료 (타입: {}, 모임장 권한)", meetingId, meeting.getType());
	}

	/**
	 * 일정 삭제 (트랜잭션만 사용 - 모임장 권한으로 동시성 이슈 없음)
	 */
	@Transactional
	public void deleteMeeting(Long meetingId) {
		// 일정 조회 및 권한 검증
		Meeting meeting = meetingQueryService.getById(meetingId);
		Long userId = getCurrentUserId();
		GroupUser groupUser = getGroupUser(meeting.getGroupId(), userId);

		// 도메인 규칙 검증
		if (!meeting.canBeManagedBy(groupUser)) {
			throw new CustomException(ErrorCode.GROUP_FORBIDDEN);
		}

		// 일정 삭제 처리
		deleteMeetingWithCleanup(meeting);

		log.info("일정 {} 삭제 완료 (모임장 권한, 타입: {})", meetingId, meeting.getType());
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
	 */
	@Transactional
	public FileUploadResponseDto uploadMeetingImage(Long meetingId, MultipartFile file) {
		Long userId = getCurrentUserId();
		Meeting meeting = meetingQueryService.getById(meetingId);
		GroupUser groupUser = getGroupUser(meeting.getGroupId(), userId);

		// 도메인 규칙 검증
		if (!meeting.canUpdateImageBy(groupUser)) {
			throw new CustomException(ErrorCode.GROUP_FORBIDDEN);
		}

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
	 */
	@Transactional
	public void deleteMeetingImage(Long meetingId) {
		Long userId = getCurrentUserId();
		Meeting meeting = meetingQueryService.getById(meetingId);
		GroupUser groupUser = getGroupUser(meeting.getGroupId(), userId);

		// 도메인 규칙 검증
		if (!meeting.canDeleteImageBy(groupUser)) {
			throw new CustomException(ErrorCode.GROUP_FORBIDDEN);
		}

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
	 * 그룹 사용자 정보 조회
	 */
	private GroupUser getGroupUser(Long groupId, Long userId) {
		return groupUserQueryService.findById(groupId, userId)
			.orElseThrow(() -> new CustomException(ErrorCode.NOT_GROUP_MEMBER));
	}

	private Long getCurrentUserId() {
		CustomUserDetails principal = (CustomUserDetails) SecurityContextHolder.getContextHolderStrategy()
			.getContext()
			.getAuthentication()
			.getPrincipal();
		return principal.getUserId();
	}
}

