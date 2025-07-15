package com.onmoim.server.meeting.repository;

import com.onmoim.server.meeting.entity.Meeting;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static com.onmoim.server.meeting.entity.QMeeting.meeting;
import static com.onmoim.server.meeting.entity.QUserMeeting.userMeeting;

@RequiredArgsConstructor
public class MeetingRepositoryCustomImpl implements MeetingRepositoryCustom {

	private final JPAQueryFactory queryFactory;

	@Override
	public List<Meeting> findEmptyMeetingsByCreator(Long userId) {
		return queryFactory
			.selectFrom(meeting)
			.leftJoin(userMeeting).on(meeting.id.eq(userMeeting.meeting.id))
			.where(
				meeting.creatorId.eq(userId),
				meeting.joinCount.eq(1),
				userMeeting.user.id.eq(userId)
			)
			.fetch();
	}

}
