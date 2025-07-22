package com.onmoim.server.group.repository;

import com.onmoim.server.common.response.CommonCursorPageResponseDto;
import com.onmoim.server.group.dto.response.JoinedGroupResponseDto;
import com.onmoim.server.group.entity.QGroupUser;
import com.onmoim.server.group.entity.Status;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

import static com.onmoim.server.group.entity.QGroupUser.groupUser;
import static com.onmoim.server.group.entity.QGroup.group;
import static com.onmoim.server.category.entity.QCategory.category;
import static com.onmoim.server.location.entity.QLocation.location;
import static com.onmoim.server.meeting.entity.QMeeting.meeting;

@RequiredArgsConstructor
public class GroupUserRepositoryCustomImpl implements GroupUserRepositoryCustom {

	private final JPAQueryFactory queryFactory;

	public CommonCursorPageResponseDto<JoinedGroupResponseDto> findJoinedGroupListByUserId(Long userId, Long cursorId, int size) {

		QGroupUser groupUserSub = new QGroupUser("groupUserSub");

		BooleanBuilder where = new BooleanBuilder();
		where.and(groupUser.user.id.eq(userId));
		where.and(groupUser.status.in(Status.OWNER, Status.MEMBER));
		if (cursorId != null) {
			where.and(group.id.lt(cursorId)); // 커서 기준
		}

		Expression<Long> memberCountExpression = JPAExpressions
			.select(groupUserSub.id.countDistinct())
			.from(groupUserSub)
			.where(
				groupUserSub.group.id.eq(group.id),
				groupUserSub.status.in(Status.OWNER, Status.MEMBER)
			);

		List<JoinedGroupResponseDto> result = queryFactory
			.select(Projections.constructor(
				JoinedGroupResponseDto.class,
				group.id,
				group.name,
				group.imgUrl,
				category.name,
				groupUser.status,
				location.dong,
				memberCountExpression,
				Expressions.numberTemplate(Long.class,
					"count(distinct case when {0} > {1} then {2} end)",
					meeting.startAt, LocalDateTime.now(), meeting.id
				).as("upcomingMeetingCount")
			))
			.from(groupUser)
			.leftJoin(group).on(group.id.eq(groupUser.group.id))
			.leftJoin(group.category, category)
			.leftJoin(group.location, location)
			.leftJoin(meeting).on(meeting.group.eq(group))
			.where(where)
			.groupBy(group.id, group.name, group.imgUrl, category.name, groupUser.status, location.dong)
			.orderBy(group.id.desc())
			.limit(size + 1)
			.fetch();

		if (result.isEmpty()) {
			return CommonCursorPageResponseDto.empty();
		}

		boolean hasNext = result.size() > size;
		List<JoinedGroupResponseDto> content = hasNext ? result.subList(0, size) : result;
		Long nextCursorId = hasNext ? content.get(content.size() - 1).getGroupId() : null;

		return CommonCursorPageResponseDto.of(content, hasNext, nextCursorId);
	}

}
