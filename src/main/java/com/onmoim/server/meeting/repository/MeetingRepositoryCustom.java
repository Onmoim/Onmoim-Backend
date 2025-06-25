package com.onmoim.server.meeting.repository;

import com.onmoim.server.meeting.dto.response.CursorPageResponseDto;
import com.onmoim.server.meeting.dto.response.MeetingResponseDto;
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
}
