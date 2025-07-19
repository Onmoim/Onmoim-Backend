package com.onmoim.server.meeting.repository;

import com.onmoim.server.meeting.dto.response.CursorPageResponseDto;
import com.onmoim.server.meeting.dto.response.MeetingResponseDto;
import com.onmoim.server.meeting.entity.Meeting;
import com.onmoim.server.meeting.entity.MeetingType;

import java.util.List;

public interface MeetingRepositoryCustom {

    /**
     * 그룹의 예정된 모임 목록 조회
     */
    CursorPageResponseDto<MeetingResponseDto> findUpcomingMeetingsInGroup(
            Long groupId,
            MeetingType type,
            Long cursorId,
            int size
    );

    /**
     * 사용자가 참여한 예정된 모임 목록 조회
     */
    CursorPageResponseDto<MeetingResponseDto> findMyUpcomingMeetings(
            List<Long> meetingIds,
            Long cursorId,
            int size
    );

    /**
     * 그룹의 D-day가 가까운 일정 조회
     */
    List<Meeting> findUpcomingMeetingsByDday(Long groupId, int limit);

	/**
	 * 참석자가 일정 생성자 본인 1명인 일정 조회
	 */
	List<Meeting> findEmptyMeetingsByCreator(Long userId);

}
