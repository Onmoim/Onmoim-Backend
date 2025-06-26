package com.onmoim.server.meeting.service;

import static java.lang.Boolean.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;
import com.onmoim.server.common.s3.service.FileStorageService;
import com.onmoim.server.group.entity.Group;
import com.onmoim.server.group.implement.GroupQueryService;
import com.onmoim.server.meeting.dto.MeetingDetail;
import com.onmoim.server.meeting.dto.request.MeetingCreateRequestDto;
import com.onmoim.server.meeting.dto.request.MeetingUpdateRequestDto;
import com.onmoim.server.meeting.entity.Meeting;
import com.onmoim.server.meeting.entity.UserMeeting;
import com.onmoim.server.meeting.repository.lock.MeetingLockRepository;
import com.onmoim.server.meeting.repository.MeetingRepository;
import com.onmoim.server.meeting.repository.UserMeetingRepository;
import com.onmoim.server.security.CustomUserDetails;
import com.onmoim.server.user.entity.User;
import com.onmoim.server.user.service.UserQueryService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;


/**
 * 일정 관리 서비스
 *
 * 트랜잭션 : TransactionTemplate 사용
 * - 이미지 업로드는 트랜잭션 밖에서 처리
 * - DB 작업만 짧은 트랜잭션으로 처리
 *
 */
@Slf4j
@Service
public class MeetingService {

	private static final String LOCK_KEY_PREFIX = "meeting::";
	private static final int LOCK_TIMEOUT_SECONDS = 5;

	private final MeetingRepository meetingRepository;
	private final MeetingQueryService meetingQueryService;
	private final UserQueryService userQueryService;
	private final GroupQueryService groupQueryService;
	private final UserMeetingRepository userMeetingRepository;
	private final FileStorageService fileStorageService;
	private final MeetingAuthService meetingAuthService;
	private final TransactionTemplate transactionTemplate;
	private final DataSource dataSource;
	private final MeetingLockRepository meetingLockRepository;

	public MeetingService(MeetingRepository meetingRepository, MeetingQueryService meetingQueryService, UserQueryService userQueryService, GroupQueryService groupQueryService, UserMeetingRepository userMeetingRepository, FileStorageService fileStorageService, MeetingAuthService meetingAuthService, TransactionTemplate transactionTemplate, DataSource dataSource, MeetingLockRepository meetingLockRepository) {
		this.meetingRepository = meetingRepository;
		this.meetingQueryService = meetingQueryService;
		this.userQueryService = userQueryService;
		this.groupQueryService = groupQueryService;
		this.userMeetingRepository = userMeetingRepository;
		this.fileStorageService = fileStorageService;
		this.meetingAuthService = meetingAuthService;
		this.transactionTemplate = transactionTemplate;
		this.dataSource = new LazyConnectionDataSourceProxy(dataSource);
		this.meetingLockRepository = meetingLockRepository;
	}

	/**
	 * 일정 생성 (이미지 포함)
	 */
	public Long createMeeting(Long groupId, MeetingCreateRequestDto request, MultipartFile image) {
		Long userId = getCurrentUserId();
		User user = userQueryService.findById(userId);

		// 1. DB에 일정 정보 먼저 저장
		Long meetingId = executeCreateMeeting(groupId, request, userId, user);

		// 2. 이미지 업로드 후, 이미지 URL 업데이트
		if (image != null && !image.isEmpty()) {
			try {
				String imageUrl = fileStorageService.uploadFile(image, "meetings").getFileUrl();
				transactionTemplate.executeWithoutResult(status -> {
					Meeting meeting = meetingQueryService.getById(meetingId);
					meeting.updateImage(imageUrl);
				});
				log.info("일정 {} 생성 후 이미지 업로드 완료: {}", meetingId, imageUrl);
			} catch (Exception e) {
				// 이미지 업로드/업데이트 실패 시, 이미 생성된 일정은 그대로 두되 경고 로그만 남김
				log.warn("일정 {}은(는) 생성되었지만, 이미지 업로드/업데이트에 실패했습니다: {}", meetingId, e.getMessage());
			}
		}

		return meetingId;
	}

	/**
	 * 일정 생성 DB 작업 (이미지 URL 제외)
	 */
	private Long executeCreateMeeting(Long groupId, MeetingCreateRequestDto request,
									  Long userId, User user) {
		return transactionTemplate.execute(status -> {
			Group group = groupQueryService.getById(groupId);
			User creator = userQueryService.findById(userId);

			Meeting meeting = Meeting.meetingCreateBuilder()
					.group(group)
					.type(request.getType())
					.title(request.getTitle())
					.startAt(request.getStartAt())
					.placeName(request.getPlaceName())
					.geoPoint(request.getGeoPoint())
					.capacity(request.getCapacity())
					.cost(request.getCost())
					.creator(creator)
					.build();

			meetingAuthService.validateCreatePermission(groupId, userId, meeting);

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
	public void joinMeeting(Long meetingId) {
		Long userId = getCurrentUserId();

		if (userMeetingRepository.existsByMeetingIdAndUserId(meetingId, userId)) {
			throw new CustomException(ErrorCode.MEETING_ALREADY_JOINED);
		}

		String lockKey = LOCK_KEY_PREFIX + meetingId;
		Connection conn = DataSourceUtils.getConnection(dataSource);

		try {
			conn.setAutoCommit(true);
			if (!meetingLockRepository.getLock(conn, lockKey, LOCK_TIMEOUT_SECONDS)) {
				throw new CustomException(ErrorCode.MEETING_LOCK_TIMEOUT);
			}

			conn.setAutoCommit(false);
			transactionTemplate.executeWithoutResult(status -> {
				Meeting meeting = meetingQueryService.getById(meetingId);
				if (!meeting.canJoin()) {
					throw new CustomException(ErrorCode.MEETING_ALREADY_CLOSED);
				}
				if (userMeetingRepository.existsByMeetingIdAndUserId(meetingId, userId)) {
					return;
				}

				User user = userQueryService.findById(userId);
				meeting.join();
				UserMeeting userMeeting = UserMeeting.create(meeting, user);
				userMeetingRepository.save(userMeeting);

				log.info("사용자 {}가 일정 {}에 참석 신청했습니다. (타입: {}, 참석: {}/{})",
						userId, meetingId, meeting.getType(), meeting.getJoinCount(), meeting.getCapacity());
			});

		} catch (SQLException e) {
			throw new CustomException(ErrorCode.LOCK_SYSTEM_ERROR, "데이터베이스 연결 또는 락 처리 중 오류가 발생했습니다: " + e.getMessage());
		} finally {
			try {
				if (conn != null) {
					conn.setAutoCommit(true);
					meetingLockRepository.releaseLock(conn, lockKey);
				}
			} catch (SQLException e) {
				log.error("락 해제 또는 커넥션 원상 복구 중 오류 발생", e);
			} finally {
				DataSourceUtils.releaseConnection(conn, dataSource);
			}
		}
	}

	/**
	 * 일정 참석 취소
	 */
	public void leaveMeeting(Long meetingId) {
		Long userId = getCurrentUserId();
		String lockKey = LOCK_KEY_PREFIX + meetingId;
		Connection conn = DataSourceUtils.getConnection(dataSource);

		try {
			conn.setAutoCommit(true);
			if (!meetingLockRepository.getLock(conn, lockKey, LOCK_TIMEOUT_SECONDS)) {
				throw new CustomException(ErrorCode.MEETING_LOCK_TIMEOUT);
			}

			conn.setAutoCommit(false);
			transactionTemplate.executeWithoutResult(status -> {
				UserMeeting userMeeting = userMeetingRepository.findByMeetingIdAndUserId(meetingId, userId).orElse(null);
				if (userMeeting == null) {
					return;
				}

				Meeting meeting = meetingQueryService.getById(meetingId);
				meeting.leave();
				userMeetingRepository.delete(userMeeting);

				if (meeting.shouldBeAutoDeleted()) {
					autoDeleteMeetingWithResources(meeting);
					log.info("일정 {} 자동 삭제 완료 (참석자 {}명 이하)", meetingId, meeting.getJoinCount());
				} else {
					log.info("사용자 {}가 일정 {}에서 참석 취소했습니다. (타입: {}, 참석: {}/{})",
							userId, meetingId, meeting.getType(), meeting.getJoinCount(), meeting.getCapacity());
				}
			});
		} catch (SQLException e) {
			throw new CustomException(ErrorCode.LOCK_SYSTEM_ERROR, "데이터베이스 연결 또는 락 처리 중 오류가 발생했습니다: " + e.getMessage());
		} finally {
			try {
				if (conn != null) {
					conn.setAutoCommit(true);
					meetingLockRepository.releaseLock(conn, lockKey);
				}
			} catch (SQLException e) {
				log.error("락 해제 또는 커넥션 원상 복구 중 오류 발생", e);
			} finally {
				DataSourceUtils.releaseConnection(conn, dataSource);
			}
		}
	}

	/**
	 * 일정 정보 수정
	 */
	public void updateMeeting(Long meetingId, MeetingUpdateRequestDto request) {
		String lockKey = LOCK_KEY_PREFIX + meetingId;
		Connection conn = DataSourceUtils.getConnection(dataSource);

		try {
			conn.setAutoCommit(true);
			if (!meetingLockRepository.getLock(conn, lockKey, LOCK_TIMEOUT_SECONDS)) {
				throw new CustomException(ErrorCode.MEETING_LOCK_TIMEOUT);
			}

			conn.setAutoCommit(false);
			transactionTemplate.executeWithoutResult(status -> {
				Long userId = getCurrentUserId();
				Meeting meeting = meetingQueryService.getById(meetingId);
				meetingAuthService.validateManagePermission(meeting, userId);

				meeting.updateMeetingInfo(
						request.getTitle(),
						request.getStartAt(),
						request.getPlaceName(),
						request.getGeoPoint(),
						request.getCapacity(),
						request.getCost()
				);
			});
		} catch (SQLException e) {
			throw new CustomException(ErrorCode.LOCK_SYSTEM_ERROR, "데이터베이스 연결 또는 락 처리 중 오류가 발생했습니다: " + e.getMessage());
		} finally {
			try {
				if (conn != null) {
					conn.setAutoCommit(true);
					meetingLockRepository.releaseLock(conn, lockKey);
				}
			} catch (SQLException e) {
				log.error("락 해제 또는 커넥션 원상 복구 중 오류 발생", e);
			} finally {
				DataSourceUtils.releaseConnection(conn, dataSource);
			}
		}
	}

	/**
	 * 일정 이미지 수정
	 */
	@Transactional
	public void updateMeetingImage(Long meetingId, MultipartFile image) {
		Long userId = getCurrentUserId();
		Meeting meeting = meetingQueryService.getById(meetingId);
		meetingAuthService.validateManagePermission(meeting, userId);

		// 기존 이미지 삭제
		final String oldImageUrl = meeting.getImgUrl();
		if (oldImageUrl != null) {
			tryDeleteFileFromS3(oldImageUrl);
		}

		// 새 이미지 업로드 및 URL 업데이트
		String newImageUrl = fileStorageService.uploadFile(image, "meetings").getFileUrl();
		meeting.updateImage(newImageUrl);
		log.info("일정 {} 이미지 수정 완료: {}", meetingId, newImageUrl);
	}

	/**
	 * 일정 삭제
	 */
	@Transactional
	public void deleteMeeting(Long meetingId) {
		Long userId = getCurrentUserId();
		Meeting meeting = meetingQueryService.getById(meetingId);
		meetingAuthService.validateManagePermission(meeting, userId);

		String imageUrl = meeting.getImgUrl();
		userMeetingRepository.deleteByMeetingId(meeting.getId());
		meeting.softDelete();

		if (imageUrl != null) {
			tryDeleteFileFromS3(imageUrl);
		}

		log.info("일정 {} 삭제 완료 (모임장 권한, 타입: {})", meetingId, meeting.getType());
	}

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

	public List<MeetingDetail> getUpcomingMeetings(
		int limit,
		Long groupId
	)
	{
		// d-day 가까운 순서로 모임 일정 조회
		List<Meeting> meetings = meetingQueryService.getUpcomingMeetingsByDday(groupId,limit);

		// 일정 id 추출
		List<Long> meetingIds = meetings.stream().map(Meeting::getId).toList();

		// 현재 사용자 참석 여부 확인을 위한 UserMeeting 조회
		List<UserMeeting> userMeetings = meetingQueryService.getUserMeetings(getCurrentUserId(), meetingIds);

		// key: meetingId value: Meeting
		Map<Long, Meeting> meetingMap = meetings.stream()
			.collect(Collectors.toMap(Meeting::getId, Function.identity()));

		// key: meetingId value: boolean(참석 여부)
		Map<Long, Boolean> userMeetingMap = userMeetings.stream()
			.collect(Collectors.toMap(um -> um.getMeeting().getId(), um -> TRUE));

		// 반환
		return meetingIds.stream()
			.map(id -> MeetingDetail.of(
				meetingMap.get(id),
				userMeetingMap.getOrDefault(id, FALSE))
			).toList();
	}

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
