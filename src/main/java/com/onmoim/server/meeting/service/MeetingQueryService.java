package com.onmoim.server.meeting.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;
import com.onmoim.server.meeting.entity.Meeting;
import com.onmoim.server.meeting.entity.MeetingStatus;
import com.onmoim.server.meeting.entity.MeetingType;
import com.onmoim.server.meeting.repository.MeetingRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeetingQueryService {
	
	private final MeetingRepository meetingRepository;

	/**
	 * 일정 ID로 조회
	 */
	public Meeting getById(Long id) {
		return meetingRepository.findByIdAndNotDeleted(id)
			.orElseThrow(() -> new CustomException(ErrorCode.NOT_EXISTS_GROUP));
	}

	/**
	 * 일정 ID로 조회 (Lock 적용)
	 */
	public Meeting getByIdWithLock(Long id) {
		return meetingRepository.findByIdAndNotDeletedWithLock(id)
			.orElseThrow(() -> new CustomException(ErrorCode.NOT_EXISTS_GROUP));
	}

	/**
	 * 그룹별 일정 목록 조회
	 */
	public Page<Meeting> findByGroupId(Long groupId, Pageable pageable) {
		return meetingRepository.findByGroupIdAndNotDeleted(groupId, pageable);
	}

	/**
	 * 그룹별 일정 목록 조회 (타입 필터링)
	 */
	public Page<Meeting> findByGroupIdAndType(Long groupId, MeetingType type, Pageable pageable) {
		if (type == null) {
			return findByGroupId(groupId, pageable);
		}
		return meetingRepository.findByGroupIdAndTypeAndNotDeleted(groupId, type, pageable);
	}

	/**
	 * 생성자별 일정 목록 조회
	 */
	public Page<Meeting> findByCreatorId(Long creatorId, Pageable pageable) {
		return meetingRepository.findByCreatorIdAndNotDeleted(creatorId, pageable);
	}

	/**
	 * 상태별 일정 조회
	 */
	public List<Meeting> findByStatus(MeetingStatus status) {
		return meetingRepository.findByStatusAndNotDeleted(status);
	}

	/**
	 * 시작 시간이 지난 일정 조회 (상태 변경용)
	 */
	public List<Meeting> findExpiredMeetings() {
		List<MeetingStatus> statuses = List.of(MeetingStatus.OPEN, MeetingStatus.FULL);
		return meetingRepository.findByStatusInAndStartAtBeforeAndNotDeleted(statuses, LocalDateTime.now());
	}

	/**
	 * 일정 저장
	 */
	@Transactional
	public Meeting save(Meeting meeting) {
		return meetingRepository.save(meeting);
	}

	/**
	 * 일정 삭제 (Soft Delete)
	 */
	@Transactional
	public void delete(Meeting meeting) {
		meeting.softDelete();
		meetingRepository.save(meeting);
	}
} 