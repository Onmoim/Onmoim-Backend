package com.onmoim.server.group.repository;

import static com.onmoim.server.category.entity.QCategory.*;
import static com.onmoim.server.group.entity.QGroup.*;
import static com.onmoim.server.group.entity.QGroupUser.*;
import static com.onmoim.server.group.entity.Status.*;
import static com.onmoim.server.location.entity.QLocation.*;
import static com.onmoim.server.meeting.entity.QMeeting.*;
import static com.onmoim.server.user.entity.QUser.*;
import static com.querydsl.core.types.ExpressionUtils.*;

import java.util.Arrays;
import java.util.List;

import com.onmoim.server.group.dto.GroupCommonInfo;
import com.onmoim.server.group.dto.GroupCommonSummary;
import com.onmoim.server.group.entity.GroupUser;
import com.onmoim.server.group.entity.Status;
import com.onmoim.server.meeting.entity.MeetingStatus;
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
		@Nullable Long cursorId,
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
				userIdGt(cursorId))
			.orderBy(groupUser.user.id.asc())
			.limit(size + 1)
			.fetch();
	}

	private BooleanExpression userIdGt(Long cursorId) {
		if(cursorId == null) return null;
		return groupUser.user.id.gt(cursorId);
	}

	/**
	 * 내 주변 인기 모임 조회
	 */
	@Override
	public List<GroupCommonSummary> readPopularGroupsNearMe(
		Long locationId,
		@Nullable Long cursorId,
		@Nullable Long memberCount,
		int size
	)
	{
		BooleanBuilder having = new BooleanBuilder();
		having.and(groupUser.count().lt(memberCount));
		having.or(groupUser.count().eq(memberCount).and(groupUser.group.id.gt(cursorId)));

		return queryFactory
			.select(Projections.constructor(
				GroupCommonSummary.class,
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
			.having(memberCountLt(cursorId), memberCountEqAndGroupIdGt(memberCount, cursorId))
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

	/**
	 * 모임 공통 조회
	 */
	@Override
	public List<GroupCommonInfo> readGroupsCommonInfo(
		List<Long> groupIds,
		Long userId
	)
	{
		return queryFactory.select(Projections.constructor(
				GroupCommonInfo.class,
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
				meeting.status.eq(MeetingStatus.OPEN))
			.where(
				group.id.in(groupIds)
			)
			.groupBy(group.id)
			.fetch();
	}
}
