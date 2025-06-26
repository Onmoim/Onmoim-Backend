package com.onmoim.server.post.entity.vo;

/**
 * 게시글 좋아요 정보 VO
 */
public record PostLikeInfo(Long likeCount, boolean isLiked) {

    public static PostLikeInfo empty() {
        return new PostLikeInfo(0L, false);
    }}
