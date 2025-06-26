package com.onmoim.server.post.util;

import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;
import com.onmoim.server.post.entity.Comment;
import com.onmoim.server.post.entity.GroupPost;

/**
 * Post 도메인 공통 검증 유틸리티
 */
public final class PostValidationUtils {

    private PostValidationUtils() {
    }

    /**
     * 엔티티가 삭제되지 않았는지 검증
     */
    public static void validateNotDeleted(Object entity, ErrorCode errorCode) {
        if (entity instanceof GroupPost post && post.getDeletedDate() != null) {
            throw new CustomException(errorCode);
        }
        if (entity instanceof Comment comment && comment.getDeletedDate() != null) {
            throw new CustomException(errorCode);
        }
    }

    /**
     * 게시글이 삭제되지 않았는지 검증
     */
    public static void validatePostNotDeleted(GroupPost post) {
        validateNotDeleted(post, ErrorCode.POST_NOT_FOUND);
    }

    /**
     * 댓글이 삭제되지 않았는지 검증
     */
    public static void validateCommentNotDeleted(Comment comment) {
        validateNotDeleted(comment, ErrorCode.COMMENT_NOT_FOUND);
    }

    /**
     * 부모 댓글인지 검증 (2단계 깊이 제한)
     */
    public static void validateParentComment(Comment comment) {
        if (!comment.isParentComment()) {
            throw new CustomException(ErrorCode.INVALID_COMMENT_THREAD);
        }
    }

    /**
     * 댓글이 동일한 게시글에 속하는지 검증
     */
    public static void validateCommentBelongsToPost(Comment comment, GroupPost post) {
        if (!comment.getPost().getId().equals(post.getId())) {
            throw new CustomException(ErrorCode.INVALID_COMMENT_THREAD);
        }
    }
}
