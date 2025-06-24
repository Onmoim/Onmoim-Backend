package com.onmoim.server.post.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;
import com.onmoim.server.post.entity.Comment;
import com.onmoim.server.post.entity.GroupPost;
import com.onmoim.server.post.repository.CommentRepository;
import com.onmoim.server.user.entity.User;

/**
 * 댓글 명령 처리 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;

    /**
     * 부모 댓글 작성
     */
    public Long createParentComment(GroupPost post, User author, String content) {
        Comment comment = Comment.builder()
                .post(post)
                .author(author)
                .parent(null) // 부모 댓글
                .content(content)
                .build();

        Comment savedComment = commentRepository.save(comment);
        return savedComment.getId();
    }

    /**
     * 답글 작성
     */
    public Long createReply(GroupPost post, User author, Long parentCommentId, String content) {
        // 부모 댓글 조회 및 검증
        Comment parentComment = commentRepository.findByIdWithAuthor(parentCommentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

        // 논리삭제 체크 (PK 조회 최적화로 애플리케이션에서 처리)
        if (parentComment.getDeletedDate() != null) {
            throw new CustomException(ErrorCode.COMMENT_NOT_FOUND);
        }

        // 부모 댓글인지 확인 (2단계 깊이만 허용)
        if (!parentComment.isParentComment()) {
            throw new CustomException(ErrorCode.INVALID_COMMENT_THREAD);
        }

        // 부모 댓글과 같은 게시글인지 확인
        if (!parentComment.getPost().getId().equals(post.getId())) {
            throw new CustomException(ErrorCode.INVALID_COMMENT_THREAD);
        }

        Comment reply = Comment.builder()
                .post(post)
                .author(author)
                .parent(parentComment)
                .content(content)
                .build();

        Comment savedReply = commentRepository.save(reply);
        return savedReply.getId();
    }

    /**
     * 댓글 수정 (작성자만 가능)
     */
    public Long updateComment(Long commentId, User author, String content) {
        Comment comment = commentRepository.findByIdAndAuthor(commentId, author)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

        // 논리삭제 체크 (PK 조회 최적화로 애플리케이션에서 처리)
        if (comment.getDeletedDate() != null) {
            throw new CustomException(ErrorCode.COMMENT_NOT_FOUND);
        }

        comment.updateContent(content);
        return comment.getId();
    }

    /**
     * 댓글 삭제 (작성자만 가능)
     */
    public Long deleteComment(Long commentId, User author) {
        Comment comment = commentRepository.findByIdAndAuthor(commentId, author)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

        // 논리삭제 체크 (PK 조회 최적화로 애플리케이션에서 처리)
        if (comment.getDeletedDate() != null) {
            throw new CustomException(ErrorCode.COMMENT_NOT_FOUND);
        }

        comment.softDelete();
        return comment.getId();
    }
} 