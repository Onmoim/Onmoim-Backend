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
 * 트랜잭션 최적화:
 * - 파일 처리 메서드에서 TransactionTemplate 사용
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
	 * 일정 생성
	 */
	@Transactional
	public Long createMeeting(Long groupId, MeetingCreateRequestDto request) {
		Long userId = getCurrentUserId();
		User user = userQueryService.findById(userId);

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

		// 권한 검증
		meetingAuthService.validateCreatePermission(groupId, userId, meeting);

		Meeting savedMeeting = meetingRepository.save(meeting);

		// 생성자는 자동으로 참석 처리 (시간 제약 없음)
		UserMeeting userMeeting = UserMeeting.create(savedMeeting, user);
		userMeetingRepository.save(userMeeting);
		savedMeeting.creatorJoin();

		log.info("사용자 {}가 모임 {}에 일정 {}을 생성했습니다.", userId, groupId, savedMeeting.getId());

		return savedMeeting.getId();
	}

	/**
	 * 일정 참석 신청 (AOP Named Lock 적용)
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

		// 조기 검증
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

		// 일정 조회 (락 보호 하에서)
		Meeting meeting = meetingQueryService.getById(meetingId);

		// 참석 여부 검증
		UserMeeting userMeeting = userMeetingRepository.findByMeetingIdAndUserId(meetingId, userId)
			.orElseThrow(() -> new CustomException(ErrorCode.MEETING_NOT_JOINED));

		meeting.leave();
		userMeetingRepository.delete(userMeeting);

		// 자동 삭제 로직
		if (meeting.shouldBeAutoDeleted()) {
			deleteMeetingWithCleanup(meeting);
			log.info("일정 {} 자동 삭제 완료 (참석자 {}명 이하)", meetingId, meeting.getJoinCount());
		} else {
			log.info("사용자 {}가 일정 {}에서 참석 취소했습니다. (AOP Lock, 타입: {}, 참석: {}/{})",
				userId, meetingId, meeting.getType(), meeting.getJoinCount(), meeting.getCapacity());
		}
	}

	/**
	 * 일정 수정
	 */
	@Transactional
	public void updateMeeting(Long meetingId, MeetingUpdateRequestDto request) {

		Meeting meeting = meetingQueryService.getById(meetingId);
		Long userId = getCurrentUserId();

		meetingAuthService.validateManagePermission(meeting, userId);

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
	 * 일정 삭제
	 */
	@Transactional
	public void deleteMeeting(Long meetingId) {

		Meeting meeting = meetingQueryService.getById(meetingId);
		Long userId = getCurrentUserId();

		meetingAuthService.validateManagePermission(meeting, userId);

		deleteMeetingWithCleanup(meeting);

		log.info("일정 {} 삭제 완료 (모임장 권한, 타입: {})", meetingId, meeting.getType());
	}

	/**
	 * 일정 삭제 및 정리 작업
	 */
	private void deleteMeetingWithCleanup(Meeting meeting) {
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
	 * 일정 이미지 업데이트
	 * - file이 null이 아니면: 이미지 업로드 로직 호출
	 * - file이 null이면: 이미지 삭제 로직 호출
	 */
	public FileUploadResponseDto updateMeetingImage(Long meetingId, MultipartFile file) {
		Long userId = getCurrentUserId();
		// 실제 로직 시작 전, Meeting 엔티티는 한 번만 조회합니다.
		Meeting meeting = meetingQueryService.getById(meetingId);

		if (file != null) {
			return uploadNewImage(userId, meeting, file);
		} else {
			deleteExistingImage(userId, meeting);
			return null; // 삭제 성공 시 별도 응답 없음
		}
	}

	/**
	 * 새 이미지를 업로드하고 기존 이미지를 교체합니다.
	 */
	private FileUploadResponseDto uploadNewImage(Long userId, Meeting meeting, MultipartFile file) {
		meetingAuthService.validateImageUploadPermission(meeting, userId);
		final String oldImageUrl = meeting.getImgUrl();

		// 1. 새 이미지 S3 업로드
		FileUploadResponseDto newImage = fileStorageService.uploadFile(file, "meetings");
		log.info("새 이미지 업로드 완료: {}", newImage.getFileUrl());

		try {
			// 2. DB에 URL 업데이트 (짧은 트랜잭션)
			updateMeetingImageUrlInTransaction(meeting.getId(), newImage.getFileUrl());

			// 3. 기존 이미지 S3에서 삭제 (성공 여부와 관계없이 진행)
			if (oldImageUrl != null) {
				deleteFileFromS3Silently(oldImageUrl);
			}
			return newImage;

		} catch (Exception dbException) {
			// 4. DB 업데이트 실패 시, 업로드했던 새 이미지 롤백
			log.warn("DB 업데이트 실패. 업로드된 S3 파일을 롤백합니다: {}", newImage.getFileUrl(), dbException);
			deleteFileFromS3Silently(newImage.getFileUrl());
			throw dbException;
		}
	}

	/**
	 * 기존 이미지의 DB 정보를 삭제하고 S3에서 파일을 제거합니다.
	 */
	private void deleteExistingImage(Long userId, Meeting meeting) {
		meetingAuthService.validateImageDeletePermission(meeting, userId);
		final String imageUrlToDelete = meeting.getImgUrl();

		if (imageUrlToDelete == null) {
			throw new CustomException(ErrorCode.MEETING_NOT_FOUND, "삭제할 이미지가 이미 존재하지 않습니다.");
		}

		// 1. DB에서 URL 먼저 null로 업데이트
		updateMeetingImageUrlInTransaction(meeting.getId(), null);

		// 2. S3에서 파일 삭제. 실패 시 에러 발생
		try {
			fileStorageService.deleteFile(imageUrlToDelete);
			log.info("S3에서 이미지 삭제 완료: {}", imageUrlToDelete);
		} catch (Exception s3Exception) {
			// S3 삭제 실패는 심각한 오류로 간주하고 로깅.
			// DB는 이미 업데이트 되었으므로 사용자 경험에는 영향이 적지만, 일관성 유지를 위해 예외를 던짐
			log.error("DB 업데이트는 성공했으나 S3 파일 삭제에 실패했습니다. 수동 확인이 필요합니다: {}", imageUrlToDelete, s3Exception);
			throw new CustomException(ErrorCode.FILE_DELETE_FAILED);
		}
	}

	/**
	 * 트랜잭션 내에서 Meeting의 이미지 URL을 업데이트하는 헬퍼 메서드
	 */
	private void updateMeetingImageUrlInTransaction(Long meetingId, String newImageUrl) {
		transactionTemplate.execute(status -> {
			Meeting freshMeeting = meetingQueryService.getById(meetingId);
			freshMeeting.updateImage(newImageUrl);
			return null;
		});
	}

	/**
	 * S3 파일 삭제를 시도하고, 실패 시 경고 로그만 남기는 헬퍼 메서드
	 */
	private void deleteFileFromS3Silently(String fileUrl) {
		try {
			fileStorageService.deleteFile(fileUrl);
			log.info("S3 파일 자동 정리 성공: {}", fileUrl);
		} catch (Exception e) {
			log.warn("S3 파일 자동 정리 실패 (무시): {}, 원인: {}", fileUrl, e.getMessage());
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

