package com.onmoim.server.meeting.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

import static com.onmoim.server.meeting.entity.QMeeting.meeting;
import static com.onmoim.server.meeting.entity.QUserMeeting.userMeeting;

@RequiredArgsConstructor
public class UserMeetingRepositoryCustomImpl implements UserMeetingRepositoryCustom {

	private final JPAQueryFactory queryFactory;

	@Override
	public List<Long> findRemainingMeetingIdsByUserId(Long userId) {
		return queryFactory
			.select(userMeeting.meeting.id)
			.from(userMeeting)
			.leftJoin(meeting).on(userMeeting.meeting.id.eq(meeting.id))
			.where(
				meeting.startAt.after(LocalDateTime.now()),
				userMeeting.user.id.eq(userId)
			)
			.fetch();
	}

}
