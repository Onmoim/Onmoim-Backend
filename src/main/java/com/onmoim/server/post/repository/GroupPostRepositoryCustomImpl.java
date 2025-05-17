package com.onmoim.server.post.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import com.onmoim.server.group.entity.Group;
import com.onmoim.server.post.dto.response.CursorPageResponseDto;
import com.onmoim.server.post.dto.response.GroupPostResponseDto;
import com.onmoim.server.post.entity.GroupPost;
import com.onmoim.server.post.entity.GroupPostType;
import com.onmoim.server.post.entity.PostImage;
import com.onmoim.server.post.entity.QGroupPost;

/**
 * 모임 게시글을 위한 커스텀 레포지토리 구현체 (Querydsl 구현)
 */
@RequiredArgsConstructor
public class GroupPostRepositoryCustomImpl
        implements GroupPostRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final PostImageRepository postImageRepository;

    @Override
    public CursorPageResponseDto<GroupPostResponseDto> findPostsWithImages(
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

        // 1. 게시글 목록만 먼저 조회 (페이징 처리)
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

        // 2. 조회된 게시글이 있으면 이미지 일괄 조회
        List<GroupPostResponseDto> postDtos = Collections.emptyList();
        if (!posts.isEmpty()) {
            // 게시글 ID 목록 추출
            List<Long> postIds = posts.stream()
                    .map(GroupPost::getId)
                    .toList();

            // 3. 이미지를 게시글 ID 기준으로 일괄 조회
            Map<Long, List<PostImage>> postImagesMap = postImageRepository
                    .findByPostIdInAndIsDeletedFalse(postIds)
                    .stream()
                    .collect(Collectors.groupingBy(pi -> pi.getPost().getId()));

            // 4. 메모리에서 게시글-이미지 매핑하여 DTO 변환
            postDtos = posts.stream()
                    .map(post -> GroupPostResponseDto.fromEntityWithImages(
                            post,
                            postImagesMap.getOrDefault(post.getId(), Collections.emptyList())
                    ))
                    .toList();
        }

        // 5. 페이징 정보와 함께 결과 반환
        return CursorPageResponseDto
                .<GroupPostResponseDto>builder()
                .content(postDtos)
                .hasNext(hasNext)
                .nextCursorId(nextCursorId)
                .build();
    }
}