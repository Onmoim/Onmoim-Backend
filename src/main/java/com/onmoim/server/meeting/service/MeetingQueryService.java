package com.onmoim.server.meeting.service;

import static com.onmoim.server.meeting.entity.QMeeting.meeting;

import com.onmoim.server.meeting.dto.response.PageResponseDto;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;
import com.onmoim.server.meeting.dto.response.MeetingResponseDto;
import com.onmoim.server.meeting.entity.Meeting;
import com.onmoim.server.meeting.entity.MeetingType;
import com.onmoim.server.meeting.repository.MeetingRepository;
import com.onmoim.server.meeting.repository.UserMeetingRepository;
import com.onmoim.server.meeting.util.CursorUtils;
import com.querydsl.core.BooleanBuilder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeetingQueryService {

	private final MeetingRepository meetingRepository;
	private final UserMeetingRepository userMeetingRepository;
	private final CursorUtils cursorUtils;

	/**
	 * 일정 ID로 조회
	 */
	public Meeting getById(Long id) {
		return meetingRepository.findByIdAndNotDeleted(id)
			.orElseThrow(() -> new CustomException(ErrorCode.NOT_EXISTS_GROUP));
	}

	//  토큰 기반 페이징 메서드들

	/**
	 * 그룹의 예정된 모임 목록 조회
	 *
	 * 클라이언트 친화적인 페이징 인터페이스
	 * - 내부적으로 Keyset-Filtering 사용
	 * - 클라이언트는 단순한 커서 토큰만 관리하면 됨
	 *
	 * @param groupId 그룹 ID
	 * @param type 모임 타입 (null이면 전체 조회)
	 * @param cursor 커서 토큰 (null이면 첫 페이지)
	 * @param size 페이지 크기
	 * @return 페이징된 모임 목록
	 */
	public PageResponseDto<MeetingResponseDto> getUpcomingMeetingsInGroup(
			Long groupId, MeetingType type, String cursor, int size) {

		ScrollPosition position = cursorUtils.decodeCursor(cursor);
		Window<MeetingResponseDto> window = getUpcomingMeetingsInGroupPage(groupId, type, position, size);

		String nextCursor = null;
		if (window.hasNext() && !window.getContent().isEmpty()) {
			MeetingResponseDto lastMeeting = window.getContent().get(window.getContent().size() - 1);
			nextCursor = cursorUtils.encodeCursor(lastMeeting.getStartAt(), lastMeeting.getId());
		}

		return PageResponseDto.from(window, nextCursor);
	}

	/**
	 * 사용자가 참여한 예정된 모임 목록 조회
	 *
	 * 클라이언트 친화적인 페이징 인터페이스
	 * - 복잡한 JOIN 로직을 커서 토큰으로 추상화
	 * - 타입 안전성과 오류 처리 내장
	 *
	 * @param userId 사용자 ID
	 * @param cursor 커서 토큰 (null이면 첫 페이지)
	 * @param size 페이지 크기
	 * @return 페이징된 모임 목록
	 */
	public PageResponseDto<MeetingResponseDto> getMyUpcomingMeetings(
			Long userId, String cursor, int size) {

		ScrollPosition position = cursorUtils.decodeCursor(cursor);
		Window<MeetingResponseDto> window = getMyUpcomingMeetingsPage(userId, position, size);

		String nextCursor = null;
		if (window.hasNext() && !window.getContent().isEmpty()) {
			MeetingResponseDto lastMeeting = window.getContent().get(window.getContent().size() - 1);
			nextCursor = cursorUtils.encodeCursor(lastMeeting.getStartAt(), lastMeeting.getId());
		}

		return PageResponseDto.from(window, nextCursor);
	}

	// 내부 헬퍼 메서드들

	/**
	 * 그룹의 예정된 모임 목록 페이징 조회 (내부 구현)
	 */
	private Window<MeetingResponseDto> getUpcomingMeetingsInGroupPage(
			Long groupId, MeetingType type, ScrollPosition position, int size) {

		LocalDateTime now = LocalDateTime.now();

		BooleanBuilder predicate = new BooleanBuilder()
			.and(meeting.groupId.eq(groupId))
			.and(meeting.startAt.gt(now))
			.and(meeting.deletedDate.isNull());

		if (type != null) {
			predicate.and(meeting.type.eq(type));
		}

		Window<Meeting> window = meetingRepository.findBy(predicate, options -> options
			.limit(size)
			.sortBy(Sort.by("startAt", "id").ascending())
			.scroll(position)
		);

		List<MeetingResponseDto> content = window.getContent().stream()
			.map(MeetingResponseDto::from)
			.toList();

		return Window.from(content, window::positionAt, window.hasNext());
	}

	/**
	 * 사용자가 참여한 예정된 모임 목록 페이징 조회 (내부 구현)
	 */
	private Window<MeetingResponseDto> getMyUpcomingMeetingsPage(
			Long userId, ScrollPosition position, int size) {

		LocalDateTime now = LocalDateTime.now();

		List<Long> meetingIds = userMeetingRepository.findMeetingIdsByUserId(userId);
		if (meetingIds.isEmpty()) {
			return Window.from(List.of(), i -> position, false);
		}

		BooleanBuilder predicate = new BooleanBuilder()
			.and(meeting.id.in(meetingIds))
			.and(meeting.startAt.gt(now))
			.and(meeting.deletedDate.isNull());

		Window<Meeting> window = meetingRepository.findBy(predicate, options -> options
			.limit(size)
			.sortBy(Sort.by("startAt", "id").ascending())
			.scroll(position)
		);

		List<MeetingResponseDto> content = window.getContent().stream()
			.map(MeetingResponseDto::from)
			.toList();

		return Window.from(content, window::positionAt, window.hasNext());
	}

	/**
	 * 일정 저장
	 */
	@Transactional
	public Meeting save(Meeting meeting) {
		return meetingRepository.save(meeting);
	}
}
