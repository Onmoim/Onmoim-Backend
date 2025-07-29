package com.onmoim.server.meeting.repository;

import com.onmoim.server.group.entity.Status;
import com.onmoim.server.meeting.dto.request.UpcomingMeetingsRequestDto;
import com.onmoim.server.meeting.dto.response.CursorPageResponseDto;
import com.onmoim.server.meeting.dto.response.MeetingResponseDto;
import com.onmoim.server.meeting.dto.response.MeetingSummaryResponseDto;
import com.onmoim.server.meeting.entity.Meeting;
import com.onmoim.server.meeting.entity.MeetingType;
import com.onmoim.server.meeting.entity.QMeeting;
import com.onmoim.server.meeting.entity.QUserMeeting;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;

import static com.onmoim.server.group.entity.QGroupUser.groupUser;
import static com.onmoim.server.meeting.entity.QMeeting.meeting;
import static com.onmoim.server.meeting.entity.QUserMeeting.userMeeting;

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

    @Override
    public List<Meeting> findUpcomingMeetingsByDday(Long groupId, int limit) {
        QMeeting q = QMeeting.meeting;

        return queryFactory
                .selectFrom(q)
                .leftJoin(q.group).fetchJoin()
                .leftJoin(q.creator).fetchJoin()
                .where(q.group.id.eq(groupId)
                        .and(q.startAt.after(LocalDateTime.now()))
                        .and(q.deletedDate.isNull()))
                .orderBy(q.startAt.asc())
                .limit(limit)
                .fetch();
    }

	@Override
	public List<Meeting> findEmptyMeetingsByCreator(Long userId) {
		return queryFactory
			.selectFrom(meeting)
			.leftJoin(userMeeting).on(meeting.id.eq(userMeeting.meeting.id))
			.where(
				meeting.creator.id.eq(userId),
				meeting.joinCount.eq(1),
				userMeeting.user.id.eq(userId)
			)
			.fetch();
	}

	@Override
	public List<MeetingSummaryResponseDto> findUpcomingMeetingList(Long userId, LocalDateTime cursorStartAt, Long cursorId, int size, UpcomingMeetingsRequestDto request) {

		BooleanBuilder where = buildUpcomingMeetingPredicate(userId, request);
		where.and(meeting.startAt.gt(LocalDateTime.now()));
		// 커서 조건: startAt > cursorStartAt or (startAt = cursorStartAt and meeting.id < cursorId)
		if (cursorStartAt != null && cursorId != null) {
			where.and(
				meeting.startAt.gt(cursorStartAt)
					.or(meeting.startAt.eq(cursorStartAt).and(meeting.id.lt(cursorId)))
			);
		}

		List<MeetingSummaryResponseDto> result = queryFactory
			.select(Projections.constructor(
				MeetingSummaryResponseDto.class,
				meeting.id,
				meeting.group.id,
				meeting.type,
				meeting.title,
				meeting.startAt,
				meeting.placeName,
				meeting.geoPoint,
				meeting.capacity,
				meeting.joinCount,
				meeting.cost,
				meeting.status,
				meeting.imgUrl,
				ExpressionUtils.as(
					JPAExpressions.selectOne()
						.from(userMeeting)
						.where(
							userMeeting.user.id.eq(userId),
							userMeeting.meeting.id.eq(meeting.id)
						)
						.exists(),
					"attendance"
				)
			))
			.from(meeting)
			.where(where)
			.orderBy(meeting.startAt.asc(), meeting.id.desc()) // 빠른 일정 순으로
			.limit(size + 1)
			.fetch();

		return result;
	}

	private BooleanBuilder buildUpcomingMeetingPredicate(Long userId, UpcomingMeetingsRequestDto request) {
		QMeeting meeting = QMeeting.meeting;
		QUserMeeting userMeeting = QUserMeeting.userMeeting;

		BooleanBuilder where = new BooleanBuilder();

		// 유저가 속한 모임의 일정만 조회
		where.and(
			meeting.group.id.in(
				JPAExpressions
					.select(groupUser.group.id)
					.from(groupUser)
					.where(
						groupUser.user.id.eq(userId),
						groupUser.status.in(Status.OWNER, Status.MEMBER)
					)
			)
		);

		// 내모임 페이지
		// 날짜 필터
		if (request.getDate() != null) {
			LocalDateTime start = request.getDate().atStartOfDay();
			LocalDateTime end = start.plusDays(1);
			where.and(meeting.startAt.between(start, end));
		}

		// 다가오는 일정 페이지
		// 이번주
		if (Boolean.TRUE.equals(request.getThisWeekYn()) &&
			(request.getThisMonthYn() == null || Boolean.FALSE.equals(request.getThisMonthYn()))) {
			LocalDate now = LocalDate.now();
			LocalDate monday = now.with(DayOfWeek.MONDAY);
			LocalDate sunday = now.with(DayOfWeek.SUNDAY);
			where.and(meeting.startAt.between(
				monday.atStartOfDay(),
				sunday.plusDays(1).atStartOfDay()
			));
		}

		// 이번달
		if (Boolean.TRUE.equals(request.getThisMonthYn())) {
			LocalDate now = LocalDate.now();
			LocalDate firstDay = now.withDayOfMonth(1);
			LocalDate lastDay = now.withDayOfMonth(now.lengthOfMonth());
			where.and(meeting.startAt.between(
				firstDay.atStartOfDay(),
				lastDay.plusDays(1).atStartOfDay()
			));
		}

		// 내가 참석한
		if (Boolean.TRUE.equals(request.getJoinedYn())) {
			where.and(
				meeting.id.in(
					JPAExpressions.select(userMeeting.meeting.id)
						.from(userMeeting)
						.where(userMeeting.user.id.eq(userId))
				)
			);
		}

		// 정기모임 / 번개모임
		if (Boolean.TRUE.equals(request.getRegularYn()) && !Boolean.TRUE.equals(request.getFlashYn())) {
			where.and(meeting.type.eq(MeetingType.REGULAR));
		} else if (Boolean.TRUE.equals(request.getFlashYn()) && !Boolean.TRUE.equals(request.getRegularYn())) {
			where.and(meeting.type.eq(MeetingType.FLASH));
		}

		return where;
	}


}
