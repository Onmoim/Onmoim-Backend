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
	 * 그룹의 예정된 모임 목록 조회 (타입 필터링 가능)
	 */
	public CursorPageResponse<MeetingResponseDto> getUpcomingMeetingsInGroup(Long groupId, MeetingType type, Long lastId, int size) {
		Pageable pageable = PageRequest.of(0, size + 1);
		LocalDateTime now = LocalDateTime.now();
		Slice<Meeting> slice = meetingRepository.findUpcomingMeetings(groupId, now, type, lastId, pageable);

		return buildCursorResponse(slice.getContent(), size);
	}

	/**
	 * 내가 참여한 예정된 모임 목록 조회
	 * No-Offset 커서 방식: lastId로 WHERE 조건 필터링 + size+1로 hasNext 판별
	 */
	public CursorPageResponse<MeetingResponseDto> getMyUpcomingMeetings(Long userId, Long lastId, int size) {
		Pageable pageable = PageRequest.of(0, size + 1);
		LocalDateTime now = LocalDateTime.now();
		Slice<Meeting> slice = meetingRepository.findUpcomingMeetingsByUser(userId, now, lastId, pageable);

		return buildCursorResponse(slice.getContent(), size);
	}

	/**
	 * 커서 기반 응답 생성 (No-Offset 방식)
	 * size+1개 조회 결과에서 hasNext 판별 후 마지막 요소 제거
	 */
	private CursorPageResponse<MeetingResponseDto> buildCursorResponse(java.util.List<Meeting> meetings, int requestedSize) {
		boolean hasNext = meetings.size() > requestedSize;

		// hasNext가 true면 마지막 요소 제거
		if (hasNext) {
			meetings.removeLast();
		}

		// 다음 커서 계산
		Long nextCursorId = meetings.isEmpty()
			? null
			: meetings.get(meetings.size() - 1).getId();

		return CursorPageResponse.<MeetingResponseDto>builder()
			.content(meetings.stream().map(MeetingResponseDto::from).toList())
			.hasNext(hasNext)
			.nextCursorId(nextCursorId)
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
