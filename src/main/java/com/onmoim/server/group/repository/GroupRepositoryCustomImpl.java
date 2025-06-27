package com.onmoim.server.group.repository;

import static com.onmoim.server.category.entity.QCategory.*;
import static com.onmoim.server.group.entity.QGroup.*;
import static com.onmoim.server.group.entity.QGroupUser.*;
import static com.onmoim.server.group.entity.Status.*;
import static com.onmoim.server.location.entity.QLocation.*;
import static com.onmoim.server.meeting.entity.QMeeting.*;
import static com.onmoim.server.user.entity.QUser.*;
import static com.querydsl.core.types.ExpressionUtils.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import com.onmoim.server.group.dto.ActiveGroup;
import com.onmoim.server.group.dto.ActiveGroupDetail;
import com.onmoim.server.group.dto.ActiveGroupRelation;
import com.onmoim.server.group.dto.PopularGroupRelation;
import com.onmoim.server.group.dto.PopularGroupSummary;
import com.onmoim.server.group.entity.GroupUser;
import com.onmoim.server.group.entity.Status;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GroupRepositoryCustomImpl implements GroupRepositoryCustom {

	private final JPAQueryFactory queryFactory;

	private final static List<Status> GROUP_MEMBER  = Arrays.asList(MEMBER, OWNER);

	@Override
	public Long countGroupMembers(Long groupId){
		return queryFactory
			.select(count(groupUser))
			.from(groupUser)
			.where(groupUser.group.id.eq(groupId), groupUser.status.in(GROUP_MEMBER))
			.fetchOne();
	}

	/**
	 * 모임 회원 조회
	 */
	@Override
	public List<GroupUser> findGroupUsers(
		Long groupId,
		@Nullable Long lastMemberId,
		int size
	)
	{
		return queryFactory
			.select(groupUser)
			.from(groupUser)
			.join(groupUser.user, user).fetchJoin()
			.where(
				groupUser.group.id.eq(groupId),
				groupUser.status.in(GROUP_MEMBER),
				userIdGt(lastMemberId))
			.orderBy(groupUser.user.id.asc())
			.limit(size + 1)
			.fetch();
	}

	private BooleanExpression userIdGt(Long lastMemberId) {
		if(lastMemberId == null) return null;
		return groupUser.user.id.gt(lastMemberId);
	}

	/**
	 * 내 주변 인기 모임 조회
	 */
	@Override
	public List<PopularGroupSummary> readPopularGroupsNearMe(
		Long locationId,
		@Nullable Long lastGroupId,
		@Nullable Long memberCount,
		int size
	)
	{
		BooleanBuilder having = new BooleanBuilder();
		having.and(groupUser.count().lt(memberCount));
		having.or(groupUser.count().eq(memberCount).and(groupUser.group.id.gt(lastGroupId)));

		return queryFactory
			.select(Projections.constructor(
				PopularGroupSummary.class,
				group.id,
				group.imgUrl,
				group.name,
				location.dong,
				category.name,
				groupUser.count()
			))
			.from(group)
			.leftJoin(category).on(group.category.id.eq(category.id))
			.leftJoin(location).on(group.location.id.eq(location.id))
			.leftJoin(groupUser).on(group.id.eq(groupUser.group.id), groupUser.status.in(GROUP_MEMBER))
			.where(group.deletedDate.isNull(), group.location.id.eq(locationId))
			.groupBy(group.id)
			.having(memberCountLt(lastGroupId), memberCountEqAndGroupIdGt(memberCount, lastGroupId))
			.orderBy(groupUser.count().desc(), group.id.asc())
			.limit(size + 1)
			.fetch();
	}

	private BooleanExpression memberCountLt(@Nullable Long memberCount) {
		if(memberCount == null) return null;
		return groupUser.count().lt(memberCount);
	}

	private BooleanExpression memberCountEqAndGroupIdGt(
		@Nullable Long memberCount,
		@Nullable Long cursorId
	)
	{
		if(memberCount == null || cursorId == null) return null;
		return groupUser.count().eq(memberCount).and(groupUser.group.id.gt(cursorId));
	}

	@Override
	public List<PopularGroupRelation> readPopularGroupRelation(
		List<Long> groupIds,
		Long userId
	)
	{
		return queryFactory.select(Projections.constructor(
				PopularGroupRelation.class,
				group.id,
				groupUser.status,
				meeting.count()
			))
			.from(group)
			.leftJoin(groupUser).on(
				group.id.eq(groupUser.group.id),
				groupUser.user.id.eq(userId)
			)
			.leftJoin(meeting).on(
				group.id.eq(meeting.group.id),
				meeting.startAt.gt(LocalDateTime.now()))
			.where(
				group.id.in(groupIds)
			)
			.groupBy(group.id)
			.fetch();
	}

	/**
	 * 활동이 활발한 모임 조회
	 * 다가오는 일정 상태 상관 X + start_at > NOW() 개수
	 */
	@Override
	public List<ActiveGroup> readMostActiveGroups(
		@Nullable Long lastGroupId,
		@Nullable Long meetingCount,
		int size
	)
	{
		BooleanBuilder having = new BooleanBuilder();
		having.and(meeting.count().lt(meetingCount));
		having.or(meeting.count().eq(meetingCount).and(group.id.gt(lastGroupId)));

		return queryFactory.select(Projections.constructor(
				ActiveGroup.class,
				group.id,
				meeting.count()
			))
			.from(group)
			.leftJoin(meeting).on(
				group.id.eq(meeting.group.id),
				meeting.startAt.gt(LocalDateTime.now()))
			.where(group.deletedDate.isNull())
			.groupBy(group.id)
			.having(having)
			.orderBy(meeting.count().desc(), group.id.asc())
			.limit(size + 1)
			.fetch();
	}

	@Override
	public List<ActiveGroupDetail> readGroupDetails(List<Long> groupIds) {
		return queryFactory.select(Projections.constructor(
			ActiveGroupDetail.class,
			group.id,
			group.imgUrl,
			group.name,
			location.dong,
			category.name,
			groupUser.count()
		))
		.from(group)
		.leftJoin(category).on(group.category.id.eq(category.id))
		.leftJoin(location).on(group.location.id.eq(location.id))
		.leftJoin(groupUser).on(group.id.eq(groupUser.group.id), groupUser.status.in(GROUP_MEMBER))
		.where(group.id.in(groupIds))
		.groupBy(group.id)
		.fetch();
	}

	@Override
	public List<ActiveGroupRelation> readGroupsRelation(
		List<Long> groupIds,
		Long userId
	)
	{
		return queryFactory.select(Projections.constructor(
			ActiveGroupRelation.class,
			groupUser.group.id,
			groupUser.user.id,
			groupUser.status
		))
		.from(groupUser)
		.where(groupUser.group.id.in(groupIds), groupUser.user.id.eq(userId))
		.fetch();
	}

}
