package com.onmoim.server.post.dto.internal;

import java.util.List;
import java.util.Map;
import java.util.Collections;

import com.onmoim.server.post.entity.PostImage;
import com.onmoim.server.post.vo.PostLikeInfo;

/**
 * 게시글 배치 조회 결과 DTO
 * Repository에서 Service로 배치 조회 결과를 전송하기 위한 내부 전송 객체.
 * 단순한 데이터 접근 편의 메서드는 포함하되, 복잡한 비즈니스 로직은 Service 계층에서 처리.
 */
public record PostBatchQueryResult(
    Map<Long, List<PostImage>> imagesByPostId,
    Map<Long, PostLikeInfo> likeInfoByPostId
) {

    /**
     * 특정 게시글의 이미지 목록 조회 (데이터 접근 편의 메서드)
     */
    public List<PostImage> getImagesForPost(Long postId) {
        return imagesByPostId.getOrDefault(postId, Collections.emptyList());
    }

    /**
     * 특정 게시글의 좋아요 정보 조회 (데이터 접근 편의 메서드)
     */
    public PostLikeInfo getLikeInfoForPost(Long postId) {
        return likeInfoByPostId.getOrDefault(postId, PostLikeInfo.empty());
    }

    /**
     * 특정 게시글의 좋아요 수 조회 (데이터 접근 편의 메서드)
     */
    public Long getLikeCountForPost(Long postId) {
        return getLikeInfoForPost(postId).likeCount();
    }

    /**
     * 특정 게시글의 사용자 좋아요 여부 조회 (데이터 접근 편의 메서드)
     */
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
