package com.onmoim.server.post.repository;

import com.onmoim.server.post.dto.response.CursorPageResponseDto;
import com.onmoim.server.group.entity.Group;
import com.onmoim.server.post.entity.GroupPost;
import com.onmoim.server.post.entity.GroupPostType;
import com.onmoim.server.post.entity.QGroupPost;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * 모임 게시글을 위한 커스텀 레포지토리 구현체 (Querydsl 구현)
 */
@RequiredArgsConstructor
public class GroupPostRepositoryCustomImpl implements GroupPostRepositoryCustom {

	private final JPAQueryFactory queryFactory;

	@Override
	public CursorPageResponseDto<GroupPost> findPosts(Group group, GroupPostType type, Long cursorId, int size) {
		QGroupPost qGroupPost = QGroupPost.groupPost;

		// 기본 조건: 지정된 그룹의 게시글 및 삭제되지 않은 게시글
		BooleanBuilder builder = new BooleanBuilder();
		builder.and(qGroupPost.group.id.eq(group.getId()));
		builder.and(qGroupPost.isDeleted.eq(false));

		// 타입 필터링
		if (type != null && type != GroupPostType.ALL) {
			builder.and(qGroupPost.type.eq(type));
		}

		// 커서 ID 이후 게시글 조회
		if (cursorId != null) {
			builder.and(qGroupPost.id.lt(cursorId)); // ID 기준 내림차순(최신순) 정렬
		}

		// size + 1개 조회하여 다음 페이지 존재 여부 확인
		List<GroupPost> posts = queryFactory
			.selectFrom(qGroupPost)
			.where(builder)
			.orderBy(qGroupPost.id.desc()) // 최신순 정렬
			.limit(size + 1)
			.fetch();

		boolean hasNext = posts.size() > size;

		// 다음 페이지가 있으면 마지막 항목 제거
		if (hasNext) {
			posts.remove(posts.size() - 1);
		}

		// 다음 커서 ID 설정
		Long nextCursorId = posts.isEmpty() ? null : posts.get(posts.size() - 1).getId();

		return CursorPageResponseDto.<GroupPost>builder()
			.content(posts)
			.hasNext(hasNext)
			.nextCursorId(nextCursorId)
			.build();
	}
}
