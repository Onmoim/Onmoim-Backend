package com.onmoim.server.post.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.onmoim.server.post.repository.PostLikeRepository;
import com.onmoim.server.post.dto.internal.PostLikeBatchResult;
import com.onmoim.server.post.entity.vo.PostLikeInfo;

/**
 * 게시글 좋아요 조회 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostLikeQueryService {

    private final PostLikeRepository postLikeRepository;

    /**
     * 사용자가 특정 게시글에 좋아요를 했는지 확인
     * - 비로그인 사용자(userId가 null)인 경우 false 반환
     */
    public boolean isLikedByUser(Long postId, Long userId) {
        if (userId == null) {
            return false;
        }

        return postLikeRepository.findLikedPostIdsByUserAndPostIds(
                List.of(postId), userId
        ).contains(postId);
    }

    /**
     * 특정 게시글의 좋아요 수 조회
     */
    public Long getLikeCount(Long postId) {
        return postLikeRepository.countActiveLikesByPostId(postId);
    }

    /**
     * 게시글 목록 조회를 위한 좋아요 정보 (좋아요 수 + 좋아요 여부)
     */
    public PostLikeInfo getPostLikeInfo(Long postId, Long userId) {
        Long likeCount = getLikeCount(postId);
        boolean isLiked = isLikedByUser(postId, userId);
        return new PostLikeInfo(likeCount, isLiked);
    }

    /**
     * 여러 게시글의 좋아요 정보를 한 번에 조회
     */
    public Map<Long, PostLikeInfo> getPostLikeInfoMap(List<Long> postIds, Long userId) {
        PostLikeBatchResult batchResult = getPostLikeBatchResult(postIds, userId);
        return batchResult.likeInfoByPostId();
    }

    public PostLikeBatchResult getPostLikeBatchResult(List<Long> postIds, Long userId) {
        if (postIds.isEmpty()) {
            return PostLikeBatchResult.empty();
        }
        Map<Long, Long> likeCountMap = fetchLikeCountMap(postIds);

        Set<Long> likedPostIds = Set.copyOf(getLikedPostIds(postIds, userId));

        Map<Long, PostLikeInfo> likeInfoMap = postIds.stream()
                .collect(Collectors.toMap(
                        postId -> postId,
                        postId -> new PostLikeInfo(
                                likeCountMap.getOrDefault(postId, 0L),
                                likedPostIds.contains(postId)
                        )
                ));

        return PostLikeBatchResult.of(likeInfoMap, likedPostIds);
    }

    private Map<Long, Long> fetchLikeCountMap(List<Long> postIds) {
        Map<Long, Long> likeCountMap = postLikeRepository.countActiveLikesByPostIds(postIds)
                .stream()
                .collect(Collectors.toMap(
                        PostLikeRepository.PostLikeCountProjection::getPostId,
                        PostLikeRepository.PostLikeCountProjection::getLikeCount
                ));

        return postIds.stream()
                .collect(Collectors.toMap(
                        postId -> postId,
                        postId -> likeCountMap.getOrDefault(postId, 0L)
                ));
    }

    /**
     * 사용자가 좋아요한 게시글 ID 목록 조회
     */
    private List<Long> getLikedPostIds(List<Long> postIds, Long userId) {
        if (userId == null) {
            return List.of(); // 비로그인 사용자
        }
        return postLikeRepository.findLikedPostIdsByUserAndPostIds(postIds, userId);
    }
}
