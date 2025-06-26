package com.onmoim.server.post.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;
import com.onmoim.server.post.entity.Comment;
import com.onmoim.server.post.entity.GroupPost;
import com.onmoim.server.post.repository.CommentRepository;
import com.onmoim.server.post.util.PostValidationUtils;
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
        Comment parentComment = findAndValidateParentCommentForReply(parentCommentId, post);

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
     * 답글 작성을 위한 부모 댓글 조회 및 검증
     */
    private Comment findAndValidateParentCommentForReply(Long parentCommentId, GroupPost post) {
        Comment parentComment = commentRepository.findByIdWithAuthor(parentCommentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

        PostValidationUtils.validateCommentNotDeleted(parentComment);
        PostValidationUtils.validateParentComment(parentComment);
        PostValidationUtils.validateCommentBelongsToPost(parentComment, post);

        return parentComment;
    }

    /**
     * 댓글 수정 (작성자만 가능)
     */
    public Long updateComment(Long commentId, User author, String content) {
        Comment comment = findAndValidateCommentByAuthor(commentId, author);
        comment.updateContent(content);
        return comment.getId();
    }

    /**
     * 댓글 삭제 (작성자만 가능)
     */
    public Long deleteComment(Long commentId, User author) {
        Comment comment = findAndValidateCommentByAuthor(commentId, author);
        comment.softDelete();
        return comment.getId();
    }

    /**
     * 작성자 검증을 통한 댓글 조회 및 검증
     */
    private Comment findAndValidateCommentByAuthor(Long commentId, User author) {
        Comment comment = commentRepository.findByIdAndAuthor(commentId, author)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

        PostValidationUtils.validateCommentNotDeleted(comment);
        return comment;
    }
}
