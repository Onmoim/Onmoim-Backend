package com.onmoim.server.group.repository;

import com.onmoim.server.common.response.CommonCursorPageResponseDto;
import com.onmoim.server.group.dto.response.GroupSummaryResponseDto;
import com.onmoim.server.group.entity.GroupLikeStatus;
import com.onmoim.server.group.entity.QGroupUser;
import com.onmoim.server.group.entity.Status;
import com.onmoim.server.meeting.entity.QMeeting;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

import static com.onmoim.server.category.entity.QCategory.category;
import static com.onmoim.server.group.entity.QGroup.group;
import static com.onmoim.server.group.entity.QGroupLike.groupLike;
import static com.onmoim.server.group.entity.QGroupUser.groupUser;
import static com.onmoim.server.location.entity.QLocation.location;

@RequiredArgsConstructor
public class GroupLikeRepositoryCustomImpl implements GroupLikeRepositoryCustom {

	private final JPAQueryFactory queryFactory;

	public List<GroupSummaryResponseDto> findLikedGroupList(Long userId, Long cursorId, int size) {

		QGroupUser groupUserSub = new QGroupUser("groupUserSub");
		QMeeting meetingSub = new QMeeting("meetingSub");

		BooleanBuilder where = new BooleanBuilder();
		where.and(groupLike.user.id.eq(userId));
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

		Expression<Long> upcomingMeetingCountExpression = JPAExpressions
			.select(meetingSub.id.countDistinct())
			.from(meetingSub)
			.where(
				meetingSub.group.id.eq(group.id),
				meetingSub.startAt.gt(LocalDateTime.now())
			);

		List<GroupSummaryResponseDto> result = queryFactory
			.select(Projections.constructor(
				GroupSummaryResponseDto.class,
				group.id,
				group.name,
				group.imgUrl,
				category.name,
				groupUser.status,
				new CaseBuilder()
					.when(groupLike.status.eq(GroupLikeStatus.LIKE)).then("LIKE")
					.otherwise("NONE"),
				location.dong,
				memberCountExpression,
				upcomingMeetingCountExpression
			))
			.from(groupLike)
			.leftJoin(groupLike.group, group)
			.leftJoin(groupUser).on(
				groupUser.user.eq(groupLike.user),
				groupUser.group.eq(groupLike.group)
			)
			.leftJoin(group.category, category)
			.leftJoin(group.location, location)
			.where(where)
			.orderBy(group.id.desc())
			.limit(size + 1)
			.fetch();

		return result;
	}
}
