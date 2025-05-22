package com.onmoim.server.group.repository;

import static com.onmoim.server.group.entity.QGroupUser.*;
import static com.onmoim.server.group.entity.Status.*;
import static com.onmoim.server.user.entity.QUser.*;
import static com.querydsl.core.types.ExpressionUtils.*;

import java.util.List;

import com.onmoim.server.group.dto.response.CursorPageResponseDto;
import com.onmoim.server.group.dto.response.GroupMembersResponseDto;
import com.onmoim.server.group.entity.GroupUser;
import com.onmoim.server.group.entity.Status;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GroupUserRepositoryCustomImpl implements GroupUserRepositoryCustom {
	private final JPAQueryFactory queryFactory;

	// 처음 커서 ID 없는 경우 전체 카운트 용도로 사용
	@Override
	public Long countGroupMembers(Long groupId){
		return queryFactory
			.select(count(groupUser))
			.from(groupUser)
			.where(groupIdEq(groupId), statusIn(List.of(MEMBER, OWNER)))
			.fetchOne();
	}

	/**
	 * fetch join GroupUser & User
	 * 추후 고도화: 프로젝션 적용해서 필요한 유저 데이터만 가져오도록
	 * 테스트 결과: 1번의 쿼리로 group-user & user 조인
	 * 인덱스 추가 및 쿼리 튜닝은 나중에..
	 */
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

	private BooleanBuilder buildFindGroupMembers(Long groupId, Long lastCursorId){
		return new BooleanBuilder()
			.and(groupIdEq(groupId))
			.and(userIdGt(lastCursorId))
			.and(statusIn(List.of(MEMBER, OWNER)));
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
