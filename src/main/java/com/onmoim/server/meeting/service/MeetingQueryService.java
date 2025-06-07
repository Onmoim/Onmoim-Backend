package com.onmoim.server.meeting.service;

import java.time.LocalDateTime;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.onmoim.server.common.dto.CursorPageResponse;
import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;
import com.onmoim.server.common.util.CursorPaginationHelper;
import com.onmoim.server.meeting.dto.response.MeetingResponseDto;
import com.onmoim.server.meeting.entity.Meeting;
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
	 * 그룹별 예정된 일정 목록 조회 (타입 필터링, 커서 기반)
	 */
	public CursorPageResponse<MeetingResponseDto> getMeetingsByGroupIdAndType(Long groupId, MeetingType type, Long cursorId, int size) {
		Pageable pageable = PageRequest.of(0, size);
		LocalDateTime now = LocalDateTime.now();
		Slice<Meeting> slice = meetingRepository.findUpcomingMeetingsByGroupIdAndTypeAfterCursor(groupId, now, type, cursorId, pageable);

		CursorPageResponse<Meeting> meetingPage = CursorPaginationHelper.fromSliceWithId(slice, Meeting::getId);
		return CursorPageResponse.<MeetingResponseDto>builder()
			.content(meetingPage.getContent().stream().map(MeetingResponseDto::from).toList())
			.hasNext(meetingPage.isHasNext())
			.nextCursorId(meetingPage.getNextCursorId())
			.build();
	}

	/**
	 * 사용자가 속한 모든 모임의 예정된 일정 조회 (커서 기반)
	 */
	public CursorPageResponse<MeetingResponseDto> getUpcomingMeetingsByUserId(Long userId, Long cursorId, int size) {
		Pageable pageable = PageRequest.of(0, size);
		LocalDateTime now = LocalDateTime.now();
		Slice<Meeting> slice = meetingRepository.findUpcomingMeetingsByUserIdAfterCursor(userId, now, cursorId, pageable);

		CursorPageResponse<Meeting> meetingPage = CursorPaginationHelper.fromSliceWithId(slice, Meeting::getId);
		return CursorPageResponse.<MeetingResponseDto>builder()
			.content(meetingPage.getContent().stream().map(MeetingResponseDto::from).toList())
			.hasNext(meetingPage.isHasNext())
			.nextCursorId(meetingPage.getNextCursorId())
			.build();
	}

	/**
	 * 일정 저장
	 */
	@Transactional
	public Meeting save(Meeting meeting) {
		return meetingRepository.save(meeting);
	}
}
