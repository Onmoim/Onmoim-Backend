package com.onmoim.server.post.vo;

import java.util.List;
import java.util.Map;
import java.util.Collections;

import com.onmoim.server.post.entity.PostImage;

public record PostBatchQueryResult(
    Map<Long, List<PostImage>> imagesByPostId,
    Map<Long, PostLikeInfo> likeInfoByPostId
) {

    public List<PostImage> getImagesForPost(Long postId) {
        return imagesByPostId.getOrDefault(postId, Collections.emptyList());
    }

    public PostLikeInfo getLikeInfoForPost(Long postId) {
        return likeInfoByPostId.getOrDefault(postId, PostLikeInfo.empty());
    }

    public Long getLikeCountForPost(Long postId) {
        return getLikeInfoForPost(postId).likeCount();
    }

    public boolean isLikedByUser(Long postId) {
        return getLikeInfoForPost(postId).isLiked();
    }


    public static PostBatchQueryResult of(
            Map<Long, List<PostImage>> imagesByPostId,
            Map<Long, PostLikeInfo> likeInfoByPostId
    ) {
        return new PostBatchQueryResult(
            imagesByPostId != null ? imagesByPostId : Collections.emptyMap(),
            likeInfoByPostId != null ? likeInfoByPostId : Collections.emptyMap()
        );
    }
}
