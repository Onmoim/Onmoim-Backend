package com.onmoim.server.meeting.service;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;
import com.onmoim.server.common.s3.dto.FileUploadResponseDto;
import com.onmoim.server.common.s3.service.FileStorageService;
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
 * 트랜잭션 : TransactionTemplate 사용
 * - 이미지 업로드는 트랜잭션 밖에서 처리
 * - DB 작업만 짧은 트랜잭션으로 처리
 *
 * 락 전략: AOP 기반 Named Lock
 * - @MeetingLock 어노테이션으로 동시성 제어
 * - 트랜잭션 시작 전 락 획득, 종료 후 락 해제
 * - 타입별 타임아웃: 정기모임 1초, 번개모임 3초
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingService {

	private final MeetingRepository meetingRepository;
	private final MeetingQueryService meetingQueryService;
	private final UserQueryService userQueryService;
	private final UserMeetingRepository userMeetingRepository;
	private final FileStorageService fileStorageService;
	private final MeetingAuthService meetingAuthService;
	private final TransactionTemplate transactionTemplate;

	/**
	 * 일정 생성 (이미지 포함)
	 */
	public Long createMeeting(Long groupId, MeetingCreateRequestDto request, MultipartFile image) {
		Long userId = getCurrentUserId();
		User user = userQueryService.findById(userId);

		String imageUrl = null;
		if (image != null && !image.isEmpty()) {
			FileUploadResponseDto uploadResult = fileStorageService.uploadFile(image, "meetings");
			imageUrl = uploadResult.getFileUrl();
			log.info("일정 생성용 이미지 업로드 완료: {}", imageUrl);
		}

		try {
			return executeCreateMeeting(groupId, request, userId, user, imageUrl);
		} catch (Exception e) {

			if (imageUrl != null) {
				tryDeleteFileFromS3(imageUrl);
				log.warn("일정 생성 실패로 업로드된 이미지 롤백: {}", imageUrl);
			}
			throw e;
		}
	}

	/**
	 * 일정 생성 DB 작업
	 */
	private Long executeCreateMeeting(Long groupId, MeetingCreateRequestDto request,
									  Long userId, User user, String imageUrl) {
		return transactionTemplate.execute(status -> {
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

			meetingAuthService.validateCreatePermission(groupId, userId, meeting);

			if (imageUrl != null) {
				meetingAuthService.validateImageUploadPermission(meeting, userId);
				meeting.updateImage(imageUrl);
			}

			Meeting savedMeeting = meetingRepository.save(meeting);

			// 생성자는 자동으로 참석 처리 (시간 제약 없음)
			UserMeeting userMeeting = UserMeeting.create(savedMeeting, user);
			userMeetingRepository.save(userMeeting);
			savedMeeting.creatorJoin();

			log.info("사용자 {}가 모임 {}에 일정 {}을 생성했습니다.", userId, groupId, savedMeeting.getId());

			return savedMeeting.getId();
		});
	}

	/**
	 * 일정 참석 신청
	 */
	@MeetingLock
	@Transactional
	public void joinMeeting(Long meetingId) {
		Long userId = getCurrentUserId();
		joinMeetingInternal(meetingId, userId);
	}

	/**
	 * 테스트 코드용 일정 참석 신청 (AOP Named Lock 적용, SecurityContext 우회)
	 */
	@MeetingLock
	@Transactional
	public void joinMeetingForTest(Long meetingId, Long userId) {
		joinMeetingInternal(meetingId, userId);
	}

	/**
	 * 일정 참석 신청 내부 로직
	 */
	private void joinMeetingInternal(Long meetingId, Long userId) {
		User user = userQueryService.findById(userId);

		Meeting meeting = meetingQueryService.getById(meetingId);
		if (!meeting.canJoin()) {
			throw new CustomException(ErrorCode.MEETING_ALREADY_CLOSED);
		}

		// 중복 참석 검증
		if (userMeetingRepository.existsByMeetingIdAndUserId(meetingId, userId)) {
			throw new CustomException(ErrorCode.MEETING_ALREADY_JOINED);
		}

		meeting.join();
		UserMeeting userMeeting = UserMeeting.create(meeting, user);
		userMeetingRepository.save(userMeeting);

		log.info("사용자 {}가 일정 {}에 참석 신청했습니다. (AOP Lock, 타입: {}, 참석: {}/{})",
			userId, meetingId, meeting.getType(), meeting.getJoinCount(), meeting.getCapacity());
	}

	/**
	 * 일정 참석 취소
	 */
	@MeetingLock
	@Transactional
	public void leaveMeeting(Long meetingId) {
		Long userId = getCurrentUserId();

		Meeting meeting = meetingQueryService.getById(meetingId);

		UserMeeting userMeeting = userMeetingRepository.findByMeetingIdAndUserId(meetingId, userId)
			.orElseThrow(() -> new CustomException(ErrorCode.MEETING_NOT_JOINED));

		meeting.leave();
		userMeetingRepository.delete(userMeeting);

		// 자동 삭제 로직
		if (meeting.shouldBeAutoDeleted()) {
			autoDeleteMeetingWithResources(meeting);
			log.info("일정 {} 자동 삭제 완료 (참석자 {}명 이하)", meetingId, meeting.getJoinCount());
		} else {
			log.info("사용자 {}가 일정 {}에서 참석 취소했습니다. (AOP Lock, 타입: {}, 참석: {}/{})",
				userId, meetingId, meeting.getType(), meeting.getJoinCount(), meeting.getCapacity());
		}
	}

	/**
	 * 일정 수정 (이미지 포함)
	 */
	public void updateMeeting(Long meetingId, MeetingUpdateRequestDto request, MultipartFile image) {
		Long userId = getCurrentUserId();

		Meeting meeting = meetingQueryService.getById(meetingId);
		meetingAuthService.validateManagePermission(meeting, userId);

		String newImageUrl = null;
		if (image != null && !image.isEmpty()) {
			meetingAuthService.validateImageUploadPermission(meeting, userId);
			FileUploadResponseDto uploadResult = fileStorageService.uploadFile(image, "meetings");
			newImageUrl = uploadResult.getFileUrl();
			log.info("일정 수정용 새 이미지 업로드 완료: {}", newImageUrl);
		}

		final String oldImageUrl = meeting.getImgUrl();
		try {
			executeUpdateMeeting(meetingId, request, newImageUrl);

			if (newImageUrl != null && oldImageUrl != null) {
				tryDeleteFileFromS3(oldImageUrl);
			}
		} catch (Exception e) {
			if (newImageUrl != null) {
				tryDeleteFileFromS3(newImageUrl);
				log.warn("일정 수정 실패로 새 이미지 롤백: {}", newImageUrl);
			}
			throw e;
		}

		log.info("일정 {} 수정 완료 (타입: {}, 모임장 권한)", meetingId, meeting.getType());
	}

	/**
	 * 일정 수정 DB 작업
	 */
	private void executeUpdateMeeting(Long meetingId, MeetingUpdateRequestDto request, String newImageUrl) {
		transactionTemplate.execute(status -> {
			Meeting meeting = meetingQueryService.getById(meetingId);

			meeting.updateMeetingInfo(
				request.getTitle(),
				request.getStartAt(),
				request.getPlaceName(),
				request.getGeoPoint(),
				request.getCapacity().intValue(),
				request.getCost()
			);

			if (newImageUrl != null) {
				meeting.updateImage(newImageUrl);
			}

			return null;
		});
	}

	/**
	 * 일정 삭제
	 */
	public void deleteMeeting(Long meetingId) {
		Meeting meeting = meetingQueryService.getById(meetingId);
		Long userId = getCurrentUserId();

		meetingAuthService.validateManagePermission(meeting, userId);

		String imageUrl = executeDeleteMeeting(meeting);
		
		if (imageUrl != null) {
			tryDeleteFileFromS3(imageUrl);
		}

		log.info("일정 {} 삭제 완료 (모임장 권한, 타입: {})", meetingId, meeting.getType());
	}

	/**
	 * 일정 삭제 DB 작업
	 */
	private String executeDeleteMeeting(Meeting meeting) {
		return transactionTemplate.execute(status -> {
			userMeetingRepository.deleteByMeetingId(meeting.getId());
			String imageUrl = meeting.getImgUrl();
			meeting.softDelete();
			return imageUrl; // S3 삭제용으로 반환
		});
	}

	/**
	 * 참석 취소 시 자동 삭제 (이미지 포함 즉시 처리)
	 */
	private void autoDeleteMeetingWithResources(Meeting meeting) {
		userMeetingRepository.deleteByMeetingId(meeting.getId());

		if (meeting.getImgUrl() != null) {
			try {
				fileStorageService.deleteFile(meeting.getImgUrl());
				log.info("일정 {} 이미지 삭제 완료: {}", meeting.getId(), meeting.getImgUrl());
			} catch (Exception e) {
				log.warn("일정 {} 이미지 삭제 실패: {}", meeting.getId(), e.getMessage());
			}
		}

		meeting.softDelete();
	}

	/**
	 * S3 파일 안전 삭제 (실패 시 로그만 기록)
	 */
	private void tryDeleteFileFromS3(String fileUrl) {
		try {
			fileStorageService.deleteFile(fileUrl);
			log.info("이미지 삭제 완료: {}", fileUrl);
		} catch (Exception e) {
			log.warn("이미지 삭제 실패 (계속 진행): {}", e.getMessage());
		}
	}

	private Long getCurrentUserId() {
		CustomUserDetails principal =
			(CustomUserDetails) SecurityContextHolder.getContextHolderStrategy()
				.getContext()
				.getAuthentication()
				.getPrincipal();
		return principal.getUserId();
	}
}

