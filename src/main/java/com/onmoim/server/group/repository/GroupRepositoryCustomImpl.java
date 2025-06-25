package com.onmoim.server.group.repository;

import static com.onmoim.server.group.entity.QGroupUser.*;
import static com.onmoim.server.group.entity.Status.*;
import static com.onmoim.server.user.entity.QUser.*;
import static com.querydsl.core.types.ExpressionUtils.*;

import java.util.Arrays;
import java.util.List;

import com.onmoim.server.group.entity.GroupUser;
import com.onmoim.server.group.entity.Status;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;

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
			.where(groupIdEq(groupId), statusIn(GROUP_MEMBER))
			.fetchOne();
	}

	@Override
	public List<GroupUser> findGroupUsers(Long groupId, Long cursorId, int size) {
		return queryFactory
			.select(groupUser)
			.from(groupUser)
			.join(groupUser.user, user).fetchJoin()
			.where(buildFindGroupMembers(groupId, cursorId))
			.orderBy(user.id.asc())
			.limit(size + 1)
			.fetch();
	}

	private BooleanExpression buildFindGroupMembers(Long groupId, Long lastCursorId){
		return groupIdEq(groupId)
			.and(userIdGt(lastCursorId))
			.and(statusIn(GROUP_MEMBER));
	}

	private BooleanExpression userIdGt(Long cursorId){
		if(cursorId == null) return null;
		return groupUser.id.userId.gt(cursorId);
	}

	private BooleanExpression groupIdEq(Long groupId) {
		return groupUser.id.groupId.eq(groupId);
	}

	private BooleanExpression statusIn(List<Status> statuses) {
		return groupUser.status.in(statuses);
	}
}
