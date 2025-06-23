package com.onmoim.server.post.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.onmoim.server.post.repository.PostLikeRepository;

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
     * 여러 게시글의 좋아요 수를 한 번에 조회 (목록 조회 최적화)
     * - 좋아요가 없는 게시글은 0으로 반환
     */
    public Map<Long, Long> getLikeCountMap(List<Long> postIds) {
        if (postIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, Long> likeCountMap = postLikeRepository.countActiveLikesByPostIds(postIds)
                .stream()
                .collect(Collectors.toMap(
                        PostLikeRepository.PostLikeCountProjection::getPostId,
                        PostLikeRepository.PostLikeCountProjection::getLikeCount
                ));

        // 좋아요가 없는 게시글도 0으로 채워넣기
        return postIds.stream()
                .collect(Collectors.toMap(
                        postId -> postId,
                        postId -> likeCountMap.getOrDefault(postId, 0L)
                ));
    }

    /**
     * 사용자가 여러 게시글에 좋아요를 했는지 확인 (목록 조회 최적화)
     * - 비로그인 사용자인 경우 모든 게시글에 대해 false 반환
     */
    public Map<Long, Boolean> getLikedStatusMap(List<Long> postIds, Long userId) {
        if (postIds.isEmpty()) {
            return Map.of();
        }

        if (userId == null) {
            // 비로그인 사용자는 모든 게시글에 대해 false 반환
            return postIds.stream()
                    .collect(Collectors.toMap(
                            postId -> postId,
                            postId -> false
                    ));
        }

        List<Long> likedPostIds = postLikeRepository.findLikedPostIdsByUserAndPostIds(postIds, userId);

        return postIds.stream()
                .collect(Collectors.toMap(
                        postId -> postId,
                        likedPostIds::contains
                ));
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
     * 여러 게시글의 좋아요 정보를 한 번에 조회 (목록 조회 최적화)
     */
    public Map<Long, PostLikeInfo> getPostLikeInfoMap(List<Long> postIds, Long userId) {
        Map<Long, Long> likeCountMap = getLikeCountMap(postIds);
        Map<Long, Boolean> likedStatusMap = getLikedStatusMap(postIds, userId);

        return postIds.stream()
                .collect(Collectors.toMap(
                        postId -> postId,
                        postId -> new PostLikeInfo(
                                likeCountMap.get(postId),
                                likedStatusMap.get(postId)
                        )
                ));
    }

    /**
     * 게시글 좋아요 정보 클래스
     */
    public record PostLikeInfo(Long likeCount, boolean isLiked) {}
}
