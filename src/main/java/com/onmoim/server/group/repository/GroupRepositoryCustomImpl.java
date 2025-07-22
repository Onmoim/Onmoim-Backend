package com.onmoim.server.group.repository;

import static com.onmoim.server.category.entity.QCategory.*;
import static com.onmoim.server.group.entity.QGroup.*;
import static com.onmoim.server.group.entity.QGroupUser.*;
import static com.onmoim.server.group.entity.QGroupLike.*;
import static com.onmoim.server.group.entity.Status.*;
import static com.onmoim.server.location.entity.QLocation.*;
import static com.onmoim.server.meeting.entity.QMeeting.*;
import static com.onmoim.server.user.entity.QUser.*;
import static com.onmoim.server.user.entity.QUserCategory.userCategory;
import static com.querydsl.core.types.ExpressionUtils.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Supplier;

import com.onmoim.server.group.dto.ActiveGroup;
import com.onmoim.server.group.dto.ActiveGroupDetail;
import com.onmoim.server.group.dto.ActiveGroupRelation;
import com.onmoim.server.group.dto.GroupDetail;
import com.onmoim.server.group.dto.PopularGroupRelation;
import com.onmoim.server.group.dto.PopularGroupSummary;
import com.onmoim.server.group.dto.response.GroupSummaryResponseDto;
import com.onmoim.server.group.entity.GroupLikeStatus;
import com.onmoim.server.group.entity.GroupUser;
import com.onmoim.server.group.entity.QGroupUser;
import com.onmoim.server.group.entity.Status;
import com.onmoim.server.meeting.entity.QMeeting;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GroupRepositoryCustomImpl implements GroupRepositoryCustom {

	private final JPAQueryFactory queryFactory;

	private final static List<Status> GROUP_MEMBER  = Arrays.asList(MEMBER, OWNER);

	/**
	 * 모임 상세 조회
	 * 모임, 카테고리, 위치, 현재 사용자와의 관계를 조회한다.
	 * 모임 삭제 여부 확인 포함
	 */
	@Override
	public Optional<GroupDetail> readGroupDetail(Long groupId, Long userId) {
		return Optional.ofNullable(queryFactory
			.select(Projections.constructor(
				GroupDetail.class,
				group.id,
				group.name,
				group.description,
				location.dong,
				category.name,
				group.imgUrl,
				category.iconUrl,
				group.capacity,
				groupUser.status,
				groupLike.status
			))
			.from(group)
			.leftJoin(category).on(group.category.id.eq(category.id))
			.leftJoin(location).on(group.location.id.eq(location.id))
			.leftJoin(groupUser).on(groupUser.group.id.eq(group.id), groupUser.user.id.eq(userId))
			.leftJoin(groupLike).on(groupLike.group.id.eq(group.id), groupLike.user.id.eq(userId))
			.where(group.id.eq(groupId), group.deletedDate.isNull())
			.fetchOne());
	}

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
				memberIdGt(lastMemberId))
			.orderBy(groupUser.user.id.asc())
			.limit(size + 1)
			.fetch();
	}

	private BooleanBuilder memberIdGt(Long lastMemberId) {
		return nullSafeBuilder(() -> groupUser.user.id.gt(lastMemberId));
	}

	/**
	 * 내 주변 인기 모임 조회
	 * 모임 위치가 locationId 일치 & 회원 수가 많은 순으로 조회한다.
	 */
	@Override
	public List<PopularGroupSummary> readPopularGroupsNearMe(
		Long locationId,
		@Nullable Long lastGroupId,
		@Nullable Long memberCount,
		int size
	)
	{
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
			.leftJoin(groupUser).on(groupUser.group.id.eq(group.id), groupUser.status.in(GROUP_MEMBER))
			.where(group.deletedDate.isNull(), group.location.id.eq(locationId))
			.groupBy(group.id)
			.having(popularReadCondition(lastGroupId, memberCount))
			.orderBy(groupUser.count().desc(), group.id.asc())
			.limit(size + 1)
			.fetch();
	}

	private BooleanBuilder popularReadCondition(
		@Nullable Long lastGroupId,
		@Nullable Long memberCount
	)
	{
		return memberCountLt(memberCount)
			.or(memberCountEq(memberCount).and(lastGroupIdGt(lastGroupId)));
	}

	private BooleanBuilder lastGroupIdGt(@Nullable Long lastGroupId) {
		return nullSafeBuilder(() -> group.id.gt(lastGroupId));
	}

	private BooleanBuilder memberCountLt(@Nullable Long memberCount) {
		return nullSafeBuilder(() -> groupUser.count().lt(memberCount));
	}

	private BooleanBuilder memberCountEq(@Nullable Long memberCount) {
		return nullSafeBuilder(() -> groupUser.count().eq(memberCount));
	}

	@Override
	public List<PopularGroupRelation> readPopularGroupRelation(
		List<Long> groupIds,
		Long userId
	)
	{
		if(groupIds.isEmpty()) return Collections.emptyList();

		return queryFactory.select(Projections.constructor(
				PopularGroupRelation.class,
				group.id,
				groupUser.status,
				meeting.count(),
				groupLike.status
			))
			.from(group)
			.leftJoin(groupUser).on(group.id.eq(groupUser.group.id), groupUser.user.id.eq(userId))
			.leftJoin(groupLike).on(groupLike.group.id.eq(group.id), groupLike.user.id.eq(userId))
			.leftJoin(meeting).on(group.id.eq(meeting.group.id), meeting.startAt.gt(LocalDateTime.now()))
			.where(
				group.id.in(groupIds)
			)
			.groupBy(group.id)
			.fetch();
	}

	/**
	 * 활동이 활발한 모임 조회
	 * 시작하는 일정이 현재 시간보다 이후인 개수가 많은 모임을 조회한다.
	 */
	@Override
	public List<ActiveGroup> readMostActiveGroups(
		@Nullable Long lastGroupId,
		@Nullable Long meetingCount,
		int size
	)
	{
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
			.having(activeReadCondition(lastGroupId, meetingCount))
			.orderBy(meeting.count().desc(), group.id.asc())
			.limit(size + 1)
			.fetch();
	}

	private BooleanBuilder activeReadCondition(
		@Nullable Long lastGroupId,
		@Nullable Long meetingCount
	)
	{
		return meetingCountLt(meetingCount)
			.or(meetingCountEq(meetingCount).and(lastGroupIdGt(lastGroupId)));
	}

	private BooleanBuilder meetingCountLt(@Nullable Long meetingCount)
	{
		return nullSafeBuilder(() -> meeting.count().lt(meetingCount));
	}

	private BooleanBuilder meetingCountEq(@Nullable Long meetingCount)
	{
		return nullSafeBuilder(() -> meeting.count().eq(meetingCount));
	}

	@Override
	public List<ActiveGroupDetail> readGroupDetails(List<Long> groupIds)
	{
		if(groupIds.isEmpty()) return Collections.emptyList();

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
		.join(groupUser).on(group.id.eq(groupUser.group.id), groupUser.status.in(GROUP_MEMBER))
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
		if(groupIds.isEmpty()) return Collections.emptyList();

		return queryFactory.select(Projections.constructor(
			ActiveGroupRelation.class,
			group.id,
			groupUser.user.id,
			groupUser.status,
			groupLike.status
		))
		.from(group)
		.leftJoin(groupUser).on(groupUser.user.id.eq(userId), groupUser.group.id.eq(group.id))
		.leftJoin(groupLike).on(groupLike.user.id.eq(userId), groupLike.group.id.eq(group.id))
		.where(group.id.in(groupIds))
		.fetch();
	}

	// 모임 년간 일정 개수
	@Override
	public Long readAnnualScheduleCount(Long groupId, LocalDateTime now)
	{
		int year = now.getYear();
		LocalDateTime startOfYear = LocalDateTime.of(year, 1, 1, 0, 0, 0);

		return queryFactory.select(meeting.count())
			.from(meeting)
			.where(
				meeting.group.id.eq(groupId),
				meeting.startAt.goe(startOfYear),
				meeting.startAt.lt(startOfYear.plusYears(1))
			)
			.fetchOne();
	}

	// 모임 월간 일정 개수
	@Override
	public Long readMonthlyScheduleCount(Long groupId, LocalDateTime now)
	{
		int year = now.getYear();
		int month = now.getMonthValue();
		LocalDateTime startOfMonth = LocalDateTime.of(year, month, 1, 0, 0, 0);
		return queryFactory.select(meeting.count())
			.from(meeting)
			.where(
				meeting.group.id.eq(groupId),
				meeting.startAt.goe(startOfMonth),
				meeting.startAt.lt(startOfMonth.plusMonths(1))
			)
			.fetchOne();
	}

	/**
	 * null-safe BooleanBuilder
	 * meeting.id.lt(param) 같은 NumberPath 또는 StringPath 경우
	 * param null -> IllegalArgumentException 발생
	 * meeting.count().lt(param) 같은 NumberExpression 경우
	 * param null -> NullPointerException 발생
	 */
	private BooleanBuilder nullSafeBuilder(Supplier<BooleanExpression> sp)
	{
		try {
			return new BooleanBuilder(sp.get());
		}
		catch (IllegalArgumentException | NullPointerException e) {
			return new BooleanBuilder();
		}
	}

	public List<GroupSummaryResponseDto> findRecommendedGroupListByCategory(Long userId, Long cursorId, int size) {

		QGroupUser groupUserSub = new QGroupUser("groupUserSub");
		QMeeting meetingSub = new QMeeting("meetingSub");

		BooleanBuilder where = new BooleanBuilder();
		where.and(userCategory.user.id.eq(userId));
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
				Expressions.constant("RECOMMEND"),
				location.dong,
				memberCountExpression,
				upcomingMeetingCountExpression
			))
			.from(group)
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
			.leftJoin(userCategory).on(userCategory.category.eq(group.category))
			.where(where)
			.orderBy(group.id.desc())
			.limit(size + 1)
			.fetch();

		return result;
	}

	public List<GroupSummaryResponseDto> findRecommendedGroupListByLocation(Long userId, Long cursorId, int size) {

		QGroupUser groupUserSub = new QGroupUser("groupUserSub");
		QMeeting meetingSub = new QMeeting("meetingSub");

		BooleanBuilder where = new BooleanBuilder();
		where.and(location.id.eq(user.location.id));
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
				Expressions.constant("RECOMMEND"),
				location.dong,
				memberCountExpression,
				upcomingMeetingCountExpression
			))
			.from(group)
			.leftJoin(groupUser).on(
				groupUser.group.eq(group),
				groupUser.user.id.eq(userId)
			)
			.leftJoin(groupUser.user, user)
			.leftJoin(group.category, category)
			.leftJoin(group.location, location)
			.leftJoin(groupLike).on(
				groupLike.user.eq(groupUser.user),
				groupLike.group.eq(groupUser.group)
			)
			.where(where)
			.orderBy(group.id.desc())
			.limit(size + 1)
			.fetch();

		return result;
	}

	public Set<Long> findRecommendedGroupIds(Long userId) {

		BooleanBuilder where = new BooleanBuilder();
		where.and(userCategory.user.id.eq(userId)
			.or(location.id.eq(user.location.id)));

		List<Long> result = queryFactory
			.select(group.id)
			.from(group)
			.leftJoin(groupUser).on(
				groupUser.group.eq(group),
				groupUser.user.id.eq(userId)
			)
			.leftJoin(groupUser.user, user)
			.leftJoin(group.category, category)
			.leftJoin(group.location, location)
			.leftJoin(groupLike).on(
				groupLike.user.eq(groupUser.user),
				groupLike.group.eq(groupUser.group)
			)
			.leftJoin(userCategory).on(userCategory.category.eq(group.category))
			.where(where)
			.orderBy(group.id.desc())
			.fetch();

		return new HashSet<>(result);
	}
}
