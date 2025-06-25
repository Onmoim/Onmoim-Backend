package com.onmoim.server.meeting.service;

import com.onmoim.server.meeting.dto.response.CursorPageResponseDto;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;
import com.onmoim.server.meeting.dto.response.MeetingResponseDto;
import com.onmoim.server.meeting.entity.Meeting;
import com.onmoim.server.meeting.entity.MeetingType;
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
}
