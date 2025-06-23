package com.onmoim.server.post.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;
import com.onmoim.server.post.entity.GroupPost;
import com.onmoim.server.post.entity.PostLike;
import com.onmoim.server.post.repository.PostLikeRepository;
import com.onmoim.server.user.entity.User;

/**
 * 게시글 좋아요 명령 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PostLikeService {

    private final PostLikeRepository postLikeRepository;

    /**
     * 게시글 좋아요
     * - 이미 좋아요한 경우: 활성화
     * - 처음 좋아요하는 경우: 새로 생성
     */
    public Long likePost(GroupPost post, User user) {
        PostLike postLike = postLikeRepository.findByPostAndUser(post, user)
                .orElseGet(() -> {
                    PostLike newPostLike = PostLike.builder()
                            .post(post)
                            .user(user)
                            .build();
                    return postLikeRepository.save(newPostLike);
                });

        postLike.active();
        return postLike.getId();
    }

    /**
     * 게시글 좋아요 취소
     * - 좋아요한 상태가 아니면 예외 발생
     */
    public Long unlikePost(GroupPost post, User user) {
        PostLike postLike = postLikeRepository.findByPostAndUserAndActive(post, user)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_LIKED));

        postLike.cancel();
        return postLike.getId();
    }

    /**
     * 게시글 좋아요 토글 (좋아요 상태에 따라 자동으로 좋아요/취소)
     */
    public boolean toggleLike(GroupPost post, User user) {
        PostLike postLike = postLikeRepository.findByPostAndUser(post, user)
                .orElse(null);

        if (postLike == null) {
            // 첫 좋아요
            createNewLike(post, user);
            return true;
        } else if (postLike.isActive()) {
            // 좋아요 취소
            postLike.cancel();
            return false;
        } else {
            // 다시 좋아요 (논리삭제 복구)
            postLike.active();
            return true;
        }
    }

    private PostLike createNewLike(GroupPost post, User user) {
        PostLike postLike = PostLike.builder()
                .post(post)
                .user(user)
                .build();
        return postLikeRepository.save(postLike);
    }
} 