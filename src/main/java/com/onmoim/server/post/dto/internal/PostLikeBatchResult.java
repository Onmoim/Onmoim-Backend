package com.onmoim.server.post.dto.internal;

import java.util.Map;
import java.util.Collections;
import java.util.Set;

import com.onmoim.server.post.vo.PostLikeInfo;

/**
 * 게시글 좋아요 배치 조회 결과 DTO
 * PostLikeQueryService에서 다른 Service로 배치 조회 결과를 전송하기 위한 내부 데이터 전송 객체.
 * 단순한 데이터 접근 편의 메서드는 포함하되, 복잡한 비즈니스 로직은 Service 계층에서 처리
 */
public record PostLikeBatchResult(
    Map<Long, PostLikeInfo> likeInfoByPostId,
    Set<Long> likedPostIds
) {

    /**
     * 특정 게시글의 좋아요 정보 조회
     */
    public PostLikeInfo getLikeInfo(Long postId) {
        return likeInfoByPostId.getOrDefault(postId, PostLikeInfo.empty());
    }

    /**
     * 특정 게시글의 좋아요 수 조회
     */
    public Long getLikeCount(Long postId) {
        return getLikeInfo(postId).likeCount();
    }

    /**
     * 특정 게시글의 사용자 좋아요 여부 조회
     */
    public boolean isLikedByUser(Long postId) {
        return getLikeInfo(postId).isLiked();
    }

    /**
     * 좋아요한 게시글 총 개수
     */
    public long getLikedPostCount() {
        return likedPostIds.size();
    }

    public static PostLikeBatchResult of(
            Map<Long, PostLikeInfo> likeInfoByPostId,
            Set<Long> likedPostIds
    ) {
        return new PostLikeBatchResult(
            likeInfoByPostId != null ? likeInfoByPostId : Collections.emptyMap(),
            likedPostIds != null ? likedPostIds : Collections.emptySet()
        );
    }

    public static PostLikeBatchResult empty() {
        return new PostLikeBatchResult(Collections.emptyMap(), Collections.emptySet());
    }
}
