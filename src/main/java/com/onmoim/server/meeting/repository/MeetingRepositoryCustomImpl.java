package com.onmoim.server.meeting.repository;

import com.onmoim.server.meeting.dto.response.CursorPageResponseDto;
import com.onmoim.server.meeting.dto.response.MeetingResponseDto;
import com.onmoim.server.meeting.entity.Meeting;
import com.onmoim.server.meeting.entity.MeetingType;
import com.onmoim.server.meeting.entity.QMeeting;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.*;

@RequiredArgsConstructor
public class MeetingRepositoryCustomImpl implements MeetingRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public CursorPageResponseDto<MeetingResponseDto> findUpcomingMeetingsInGroup(
            Long groupId, 
            MeetingType type, 
            Long cursorId, 
            int size
    ) {
        BooleanBuilder predicate = buildGroupMeetingPredicate(groupId, type, cursorId);
        
        Deque<Meeting> pagedMeetings = fetchPagedMeetings(predicate, size);
        
        boolean hasNext = pagedMeetings.size() > size;
        Long nextCursorId = extractNextCursor(pagedMeetings, size, hasNext);
        
        List<MeetingResponseDto> dtos = mapToDto(pagedMeetings);
        
        return CursorPageResponseDto.<MeetingResponseDto>builder()
                .content(dtos)
                .hasNext(hasNext)
                .nextCursorId(nextCursorId)
                .build();
    }

    @Override
    public CursorPageResponseDto<MeetingResponseDto> findMyUpcomingMeetings(
            List<Long> meetingIds, 
            Long cursorId, 
            int size
    ) {
        BooleanBuilder predicate = buildMyMeetingPredicate(meetingIds, cursorId);
        
        Deque<Meeting> pagedMeetings = fetchPagedMeetings(predicate, size);
        
        boolean hasNext = pagedMeetings.size() > size;
        Long nextCursorId = extractNextCursor(pagedMeetings, size, hasNext);
        
        List<MeetingResponseDto> dtos = mapToDto(pagedMeetings);
        
        return CursorPageResponseDto.<MeetingResponseDto>builder()
                .content(dtos)
                .hasNext(hasNext)
                .nextCursorId(nextCursorId)
                .build();
    }

    private BooleanBuilder buildGroupMeetingPredicate(Long groupId, MeetingType type, Long cursorId) {
        QMeeting q = QMeeting.meeting;
        BooleanBuilder b = new BooleanBuilder()
                .and(q.group.id.eq(groupId))
                .and(q.startAt.after(LocalDateTime.now()))
                .and(q.deletedDate.isNull());

        if (type != null) {
            b.and(q.type.eq(type));
        }
        if (cursorId != null) {
            b.and(q.id.lt(cursorId));
        }
        return b;
    }

    private BooleanBuilder buildMyMeetingPredicate(List<Long> meetingIds, Long cursorId) {
        QMeeting q = QMeeting.meeting;
        BooleanBuilder b = new BooleanBuilder()
                .and(q.id.in(meetingIds))
                .and(q.startAt.after(LocalDateTime.now()))
                .and(q.deletedDate.isNull());

        if (cursorId != null) {
            b.and(q.id.lt(cursorId));
        }
        return b;
    }

    private Deque<Meeting> fetchPagedMeetings(BooleanBuilder predicate, int size) {
        QMeeting q = QMeeting.meeting;
        List<Meeting> fetched = queryFactory
                .selectFrom(q)
                .leftJoin(q.group).fetchJoin()
                .leftJoin(q.creator).fetchJoin()
                .where(predicate)
                .orderBy(q.id.desc())
                .limit((long) size + 1)
                .fetch();
        return new LinkedList<>(fetched);
    }

    private Long extractNextCursor(Deque<Meeting> meetings, int size, boolean hasNext) {
        if (!hasNext) return null;
        meetings.removeLast();
        return meetings.getLast().getId();
    }

    private List<MeetingResponseDto> mapToDto(Collection<Meeting> meetings) {
        return meetings.stream()
                .map(MeetingResponseDto::from)
                .toList();
    }
} 