package com.onmoim.server.post.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;

import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import com.onmoim.server.group.entity.Group;
import com.onmoim.server.post.dto.internal.PostBatchQueryResult;
import com.onmoim.server.post.dto.internal.PostLikeBatchResult;
import com.onmoim.server.post.dto.response.CursorPageResponseDto;
import com.onmoim.server.post.dto.response.GroupPostResponseDto;
import com.onmoim.server.post.entity.GroupPost;
import com.onmoim.server.post.entity.GroupPostType;
import com.onmoim.server.post.entity.PostImage;
import com.onmoim.server.post.entity.QGroupPost;
import com.onmoim.server.post.service.PostLikeQueryService;

@RequiredArgsConstructor
public class GroupPostRepositoryCustomImpl implements GroupPostRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final PostImageRepository postImageRepository;
    private final PostLikeQueryService postLikeQueryService;


    @Override
    public CursorPageResponseDto<GroupPostResponseDto> findPostsWithImagesAndLikes(
            Group group,
            GroupPostType type,
            Long cursorId,
            int size,
            Long userId
    ) {
        BooleanBuilder predicate = buildPredicate(group, type, cursorId);

        Deque<GroupPost> pagedPosts = fetchPagedPosts(predicate, size);

        boolean hasNext = pagedPosts.size() > size;
        Long nextCursorId = extractNextCursor(pagedPosts, size, hasNext);

        List<GroupPostResponseDto> dtos = mapToDtoWithImagesAndLikes(pagedPosts, userId);

        return CursorPageResponseDto.<GroupPostResponseDto>builder()
                .content(dtos)
                .hasNext(hasNext)
                .nextCursorId(nextCursorId)
                .build();
    }

    private BooleanBuilder buildPredicate(Group group, GroupPostType type, Long cursorId) {
        QGroupPost q = QGroupPost.groupPost;
        BooleanBuilder b = new BooleanBuilder()
                .and(q.group.id.eq(group.getId()))
                .and(q.deletedDate.isNull());

        if (type != null && type != GroupPostType.ALL) {
            b.and(q.type.eq(type));
        }
        if (cursorId != null) {
            b.and(q.id.lt(cursorId));
        }
        return b;
    }

    private Deque<GroupPost> fetchPagedPosts(BooleanBuilder predicate, int size) {
        QGroupPost q = QGroupPost.groupPost;
        List<GroupPost> fetched = queryFactory
                .selectFrom(q)
                .join(q.author).fetchJoin()
                .join(q.group).fetchJoin()
                .where(predicate)
                .orderBy(q.id.desc())
                .limit((long) size + 1)
                .fetch();
        return new LinkedList<>(fetched);
    }

    private Long extractNextCursor(Deque<GroupPost> posts, int size, boolean hasNext) {
        if (!hasNext) return null;
        posts.removeLast();
        return posts.getLast().getId();
    }

    private List<GroupPostResponseDto> mapToDtoWithImagesAndLikes(Collection<GroupPost> posts, Long userId) {
        if (posts.isEmpty()) {
            return Collections.emptyList();
        }

        PostBatchQueryResult batchResult = createBatchQueryResult(posts, userId);

        return posts.stream()
                .map(post -> GroupPostResponseDto.fromEntityWithImagesAndLikes(
                        post,
                        batchResult.getImagesForPost(post.getId()),
                        batchResult.getLikeCountForPost(post.getId()),
                        batchResult.isLikedByUser(post.getId())
                ))
                .toList();
    }

    private PostBatchQueryResult createBatchQueryResult(Collection<GroupPost> posts, Long userId) {
        List<Long> postIds = posts.stream()
                .map(GroupPost::getId)
                .toList();

        Map<Long, List<PostImage>> imagesByPostId = Collections.unmodifiableMap(
                postImageRepository
                        .findByPostIdInAndIsDeletedFalse(postIds)
                        .stream()
                        .collect(Collectors.groupingBy(
                                pi -> pi.getPost().getId(),
                                Collectors.toUnmodifiableList()
                        ))
        );

        PostLikeBatchResult likeBatchResult = postLikeQueryService.getPostLikeBatchResult(postIds, userId);

        return PostBatchQueryResult.of(imagesByPostId, likeBatchResult.likeInfoByPostId());
    }
}
