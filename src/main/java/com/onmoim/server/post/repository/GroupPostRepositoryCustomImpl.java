package com.onmoim.server.post.repository;

import java.util.List;

import lombok.RequiredArgsConstructor;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;

import com.onmoim.server.group.entity.Group;
import com.onmoim.server.post.entity.GroupPost;
import com.onmoim.server.post.entity.GroupPostType;
import com.onmoim.server.post.entity.QGroupPost;
import com.onmoim.server.post.dto.response.CursorPageResponseDto;

/**
 * 모임 게시글을 위한 커스텀 레포지토리 구현체 (Querydsl 구현)
 */
@RequiredArgsConstructor
public class GroupPostRepositoryCustomImpl
        implements GroupPostRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public CursorPageResponseDto<GroupPost> findPosts(
            Group group,
            GroupPostType type,
            Long cursorId,
            int size
    ) {
        QGroupPost qGroupPost = QGroupPost.groupPost;

        BooleanBuilder builder = new BooleanBuilder();
        builder.and(qGroupPost.group.id.eq(group.getId()));
        builder.and(qGroupPost.isDeleted.eq(false));

        if (type != null && type != GroupPostType.ALL) {
            builder.and(qGroupPost.type.eq(type));
        }

        if (cursorId != null) {
            builder.and(qGroupPost.id.lt(cursorId));
        }

        List<GroupPost> posts = queryFactory
                .selectFrom(qGroupPost)
                .where(builder)
                .orderBy(qGroupPost.id.desc())
                .limit(size + 1)
                .fetch();

        boolean hasNext = posts.size() > size;
        if (hasNext) {
            posts.remove(posts.size() - 1);
        }

        Long nextCursorId = hasNext
                ? posts.get(posts.size() - 1).getId()
                : null;

        return CursorPageResponseDto
                .<GroupPost>builder()
                .content(posts)
                .hasNext(hasNext)
                .nextCursorId(nextCursorId)
                .build();
    }
}