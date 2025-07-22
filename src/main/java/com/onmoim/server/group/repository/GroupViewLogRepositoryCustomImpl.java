package com.onmoim.server.group.repository;

import com.onmoim.server.group.dto.response.GroupSummaryResponseDto;
import com.onmoim.server.group.dto.response.RecentViewedGroupSummaryResponseDto;
import com.onmoim.server.group.entity.GroupLikeStatus;
import com.onmoim.server.group.entity.QGroupUser;
import com.onmoim.server.group.entity.Status;
import com.onmoim.server.meeting.entity.QMeeting;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

import static com.onmoim.server.group.entity.QGroupViewLog.groupViewLog;
import static com.onmoim.server.group.entity.QGroupLike.groupLike;
import static com.onmoim.server.group.entity.QGroupUser.groupUser;
import static com.onmoim.server.group.entity.QGroup.group;
import static com.onmoim.server.category.entity.QCategory.category;
import static com.onmoim.server.location.entity.QLocation.location;

@RequiredArgsConstructor
public class GroupViewLogRepositoryCustomImpl implements GroupViewLogRepositoryCustom {

	private final JPAQueryFactory queryFactory;

	public List<RecentViewedGroupSummaryResponseDto> findRecentViewedGroupList(Long userId, LocalDateTime cursorViewedAt, Long cursorLogId, int size) {

		QGroupUser groupUserSub = new QGroupUser("groupUserSub");
		QMeeting meetingSub = new QMeeting("meetingSub");

		BooleanBuilder where = new BooleanBuilder();
		where.and(groupViewLog.user.id.eq(userId));
		// 커서 조건: modifiedDate < cursorViewedAt or (modifiedDate = cursorViewedAt and group.id < cursorLogId)
		if (cursorViewedAt != null && cursorLogId != null) {
			where.and(
				groupViewLog.modifiedDate.lt(cursorViewedAt)
					.or(groupViewLog.modifiedDate.eq(cursorViewedAt).and(group.id.lt(cursorLogId)))
			);
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

		List<RecentViewedGroupSummaryResponseDto> result = queryFactory
			.select(Projections.constructor(
				RecentViewedGroupSummaryResponseDto.class,
				group.id,
				group.name,
				group.imgUrl,
				category.name,
				groupUser.status,
				new CaseBuilder()
					.when(groupLike.status.eq(GroupLikeStatus.LIKE)).then("LIKE")
					.otherwise("NONE"),
				Expressions.constant(""), // 서비스에서 따로 주입
				location.dong,
				memberCountExpression,
				upcomingMeetingCountExpression,
				groupViewLog.modifiedDate
			))
			.from(groupViewLog)
			.join(groupViewLog.group, group)
			.leftJoin(groupUser).on(
				groupUser.group.eq(group),
				groupUser.user.id.eq(userId)
			)
			.leftJoin(group.category, category)
			.leftJoin(group.location, location)
			.leftJoin(groupLike).on(
				groupLike.user.eq(groupUser.user),
				groupLike.group.eq(groupUser.group)
			)
			.where(where)
			.orderBy(groupViewLog.modifiedDate.desc(), groupViewLog.groupViewLogId.desc())
			.limit(size + 1)
			.fetch();

		return result;
	}
}
