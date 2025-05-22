package com.onmoim.server.group.repository;

import static com.onmoim.server.group.entity.QGroupUser.*;
import static com.onmoim.server.user.entity.QUser.*;

import java.util.List;

import com.onmoim.server.group.entity.GroupUser;
import com.onmoim.server.group.entity.Status;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GroupUserRepositoryCustomImpl implements GroupUserRepositoryCustom {
	private final JPAQueryFactory queryFactory;
	/**
	 * fetch join GroupUser & User
	 * 추후 고도화: 프로젝션 적용해서 필요한 유저 데이터만 가져오도록
	 * 테스트 결과: 1번의 쿼리로 group-user & user 조인
	 * 인덱스 추천: (group_id, status)
	 * - group_id, status 세컨더리 인덱스로 만들면
	 * - 세컨더리 인덱스의 리프 노드에는 항상 pk(group_id, user_id)를 가지기 때문에
	 * - 인덱스 레인지 스캔 + 커버링 인덱스 효과까지 만들어냄
	 * 기타: 페이징 필요?
	 */
	@Override
	public List<GroupUser> findGroupUserAndMembers(Long groupId) {
		return queryFactory
			.select(groupUser)
			.from(groupUser)
			.join(groupUser.user, user).fetchJoin()
			.where(
				groupUser.group.id.eq(groupId),
				groupUser.status.in(List.of(Status.MEMBER, Status.OWNER)))
			.fetch();
	}
}
