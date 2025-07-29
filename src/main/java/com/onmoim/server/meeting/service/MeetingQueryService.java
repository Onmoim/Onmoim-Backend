package com.onmoim.server.meeting.service;

import com.onmoim.server.meeting.dto.request.UpcomingMeetingsRequestDto;
import com.onmoim.server.meeting.dto.response.CursorPageResponseDto;

import java.time.LocalDateTime;
import java.util.List;

import com.onmoim.server.meeting.dto.response.MeetingSummaryResponseDto;
import com.onmoim.server.meeting.dto.response.UpcomingMeetingCursorPageResponseDto;
import com.onmoim.server.security.CustomUserDetails;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;
import com.onmoim.server.meeting.dto.response.MeetingResponseDto;
import com.onmoim.server.meeting.entity.Meeting;
import com.onmoim.server.meeting.entity.MeetingType;
import com.onmoim.server.meeting.entity.UserMeeting;
import com.onmoim.server.meeting.repository.MeetingRepository;
import com.onmoim.server.meeting.repository.UserMeetingRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeetingQueryService {

	private final MeetingRepository meetingRepository;
	private final UserMeetingRepository userMeetingRepository;

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
	 */
	public CursorPageResponseDto<MeetingResponseDto> getUpcomingMeetingsInGroup(
			Long groupId, MeetingType type, Long cursorId, int size) {

		return meetingRepository.findUpcomingMeetingsInGroup(groupId, type, cursorId, size);
	}

	/**
	 * 사용자가 참여한 예정된 모임 목록 조회
	 */
	public CursorPageResponseDto<MeetingResponseDto> getMyUpcomingMeetings(
			Long userId, Long cursorId, int size) {

		List<Long> meetingIds = userMeetingRepository.findMeetingIdsByUserId(userId);
		if (meetingIds.isEmpty()) {
			return CursorPageResponseDto.<MeetingResponseDto>builder()
				.content(List.of())
				.hasNext(false)
				.nextCursorId(null)
				.build();
		}

		return meetingRepository.findMyUpcomingMeetings(meetingIds, cursorId, size);
	}

	/**
	 * 일정 저장
	 */
	@Transactional
	public Meeting save(Meeting meeting) {
		return meetingRepository.save(meeting);
	}

	/**
	 * 특정 그룹의 D-day가 가까운 일정 조회
	 *
	 * @param groupId 그룹 ID
	 * @param limit 조회할 일정 수
	 * @return D-day가 가까운 일정 목록
	 */
	public List<Meeting> getUpcomingMeetingsByDday(Long groupId, int limit) {
		return meetingRepository.findUpcomingMeetingsByDday(groupId, limit);
	}

	public List<UserMeeting> getUserMeetings(Long userId, List<Long> meetingIds) {
		return userMeetingRepository.findByUserAndMeetings(userId, meetingIds);
	}

	/**
	 * 다가오는 일정 조회
	 */
	public UpcomingMeetingCursorPageResponseDto<MeetingSummaryResponseDto> getUpcomingMeetingList(LocalDateTime startAt, Long cursorId, int size, UpcomingMeetingsRequestDto request) {
		Long userId = getCurrentUserId();

		List<MeetingSummaryResponseDto> result = meetingRepository.findUpcomingMeetingList(userId, startAt, cursorId, size + 1, request);

		if (result.isEmpty()) {
			return UpcomingMeetingCursorPageResponseDto.empty();
		}

		boolean hasNext = result.size() > size;
		List<MeetingSummaryResponseDto> content = hasNext ? result.subList(0, size) : result;

		// 커서 추출
		LocalDateTime nextCursorStartAt = hasNext ? content.get(content.size() - 1).getStartAt() : null;
		Long nextCursorId = hasNext ? content.get(content.size() - 1).getId() : null;

		return UpcomingMeetingCursorPageResponseDto.of(content, hasNext, nextCursorStartAt, nextCursorId);
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
