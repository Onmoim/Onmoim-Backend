package com.onmoim.server.post.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;
import com.onmoim.server.post.constant.PostConstants;
import com.onmoim.server.post.dto.response.CommentResponseDto;
import com.onmoim.server.post.dto.response.CommentThreadResponseDto;
import com.onmoim.server.post.dto.response.CursorPageResponseDto;
import com.onmoim.server.post.entity.Comment;
import com.onmoim.server.post.entity.GroupPost;
import com.onmoim.server.post.repository.CommentRepository;
import com.onmoim.server.post.util.PostValidationUtils;

/**
 * 댓글 조회 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentQueryService {

    private final CommentRepository commentRepository;

    /**
     * 게시글의 부모댓글 목록 조회
     */
    public CursorPageResponseDto<CommentResponseDto> getParentComments(
            GroupPost post,
            Long cursor
    ) {
        List<Comment> comments = commentRepository.findParentCommentsByPost(post, cursor);

        List<Comment> limitedComments = applyPagination(comments);
        boolean hasMore = comments.size() > PostConstants.CURSOR_PAGE_SIZE;

        List<Long> commentIds = extractCommentIds(limitedComments);
        Map<Long, Long> replyCountMap = getReplyCountMap(commentIds);

        List<CommentResponseDto> responseDtos = convertToCommentDtos(limitedComments, replyCountMap);

        Long nextCursor = calculateNextCursor(hasMore, limitedComments);

        return CursorPageResponseDto.<CommentResponseDto>builder()
                .content(responseDtos)
                .nextCursorId(nextCursor)
                .hasNext(hasMore)
                .build();
    }

    /**
     * 특정 댓글의 답글 목록 조회
     */
    public CommentThreadResponseDto getCommentThread(Long commentId, Long cursor) {

        Comment parentComment = findAndValidateParentComment(commentId);

        List<Comment> replies = commentRepository.findRepliesByParent(parentComment, cursor);
        List<Comment> limitedReplies = applyPagination(replies);
        boolean hasMore = replies.size() > PostConstants.CURSOR_PAGE_SIZE;

        CommentResponseDto parentDto = CommentResponseDto.from(
            parentComment,
            commentRepository.countRepliesByParentId(commentId)
        );

        List<CommentResponseDto> replyDtos = limitedReplies.stream()
                .map(CommentResponseDto::fromReply)
                .toList();

        Long nextCursor = calculateNextCursor(hasMore, limitedReplies);

        return CommentThreadResponseDto.of(parentDto, replyDtos, nextCursor, hasMore);
    }

    private List<Comment> applyPagination(List<Comment> comments) {
        List<Comment> limitedComments = comments.stream()
                .limit(PostConstants.CURSOR_PAGE_SIZE + 1)
                .toList();

        boolean hasMore = limitedComments.size() > PostConstants.CURSOR_PAGE_SIZE;
        if (hasMore) {
            limitedComments = limitedComments.subList(0, PostConstants.CURSOR_PAGE_SIZE);
        }

        return limitedComments;
    }

    private List<Long> extractCommentIds(List<Comment> comments) {
        return comments.stream()
                .map(Comment::getId)
                .toList();
    }

    private List<CommentResponseDto> convertToCommentDtos(
            List<Comment> comments,
            Map<Long, Long> replyCountMap
    ) {
        return comments.stream()
                .map(comment -> CommentResponseDto.from(
                    comment,
                    replyCountMap.getOrDefault(comment.getId(), 0L)
                ))
                .toList();
    }

    private Long calculateNextCursor(boolean hasMore, List<Comment> comments) {
        return hasMore && !comments.isEmpty()
                ? comments.get(comments.size() - 1).getId()
                : null;
    }

    /**
     * 부모 댓글 조회 및 검증
     */
    private Comment findAndValidateParentComment(Long commentId) {
        Comment parentComment = commentRepository.findByIdWithAuthor(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

        PostValidationUtils.validateCommentNotDeleted(parentComment);
        PostValidationUtils.validateParentComment(parentComment);

        return parentComment;
    }

    /**
     * 여러 부모댓글의 답글 개수를 한 번에 조회
     */
    private Map<Long, Long> getReplyCountMap(List<Long> parentIds) {
        if (parentIds.isEmpty()) {
            return Map.of();
        }

        return commentRepository.countRepliesByParentIds(parentIds)
                .stream()
                .collect(Collectors.toMap(
                    objects -> (Long) objects[0],  // parentId
                    objects -> (Long) objects[1]   // replyCount
                ));
    }
}
