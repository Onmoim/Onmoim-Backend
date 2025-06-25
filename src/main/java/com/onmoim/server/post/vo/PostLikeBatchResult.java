package com.onmoim.server.post.vo;
import java.util.Map;
import java.util.Collections;
import java.util.Set;


/**
 * 게시글 좋아요 배치 조회 결과 VO
 */
public record PostLikeBatchResult(
    Map<Long, PostLikeInfo> likeInfoByPostId,
    Set<Long> likedPostIds
) {

    public PostLikeInfo getLikeInfo(Long postId) {
        return likeInfoByPostId.getOrDefault(postId, PostLikeInfo.empty());
    }

    public Long getLikeCount(Long postId) {
        return getLikeInfo(postId).likeCount();
    }

    public boolean isLikedByUser(Long postId) {
        return getLikeInfo(postId).isLiked();
    }

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
