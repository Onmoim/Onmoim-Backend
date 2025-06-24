package com.onmoim.server.post.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;
import com.onmoim.server.post.dto.response.CommentResponseDto;
import com.onmoim.server.post.dto.response.CommentThreadResponseDto;
import com.onmoim.server.post.dto.response.CursorPageResponseDto;
import com.onmoim.server.post.entity.Comment;
import com.onmoim.server.post.entity.GroupPost;
import com.onmoim.server.post.repository.CommentRepository;

/**
 * 댓글 조회 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentQueryService {

    private final CommentRepository commentRepository;

    private static final int DEFAULT_PAGE_SIZE = 20;

    /**
     * 게시글의 부모댓글 목록 조회 (커서 페이지네이션)
     */
    public CursorPageResponseDto<CommentResponseDto> getParentComments(
            GroupPost post,
            Long cursor
    ) {
        List<Comment> comments = commentRepository.findParentCommentsByPost(
            post, cursor
        );

        // 페이지 크기로 제한
        List<Comment> limitedComments = comments.stream()
                .limit(DEFAULT_PAGE_SIZE + 1)
                .collect(Collectors.toList());

        boolean hasMore = limitedComments.size() > DEFAULT_PAGE_SIZE;
        if (hasMore) {
            limitedComments = limitedComments.subList(0, DEFAULT_PAGE_SIZE);
        }

        // 답글 개수 배치 조회 (N+1 문제 해결)
        List<Long> commentIds = limitedComments.stream()
                .map(Comment::getId)
                .collect(Collectors.toList());

        Map<Long, Long> replyCountMap = getReplyCountMap(commentIds);

        // DTO 변환
        List<CommentResponseDto> responseDtos = limitedComments.stream()
                .map(comment -> CommentResponseDto.from(
                    comment,
                    replyCountMap.getOrDefault(comment.getId(), 0L)
                ))
                .collect(Collectors.toList());

        Long nextCursor = hasMore && !limitedComments.isEmpty()
                ? limitedComments.get(limitedComments.size() - 1).getId()
                : null;

        return CursorPageResponseDto.<CommentResponseDto>builder()
                .content(responseDtos)
                .nextCursorId(nextCursor)
                .hasNext(hasMore)
                .build();
    }

    /**
     * 특정 댓글의 답글 목록 조회 (커서 페이지네이션)
     */
    public CommentThreadResponseDto getCommentThread(
            Long commentId,
            Long cursor
    ) {
        // 부모 댓글 조회
        Comment parentComment = commentRepository.findByIdWithAuthor(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

        // 논리삭제 체크 (PK 조회 최적화로 애플리케이션에서 처리)
        if (parentComment.getDeletedDate() != null) {
            throw new CustomException(ErrorCode.COMMENT_NOT_FOUND);
        }

        if (!parentComment.isParentComment()) {
            throw new CustomException(ErrorCode.INVALID_COMMENT_THREAD);
        }

        // 답글 목록 조회
        List<Comment> replies = commentRepository.findRepliesByParent(
            parentComment, cursor
        );

        // 페이지 크기로 제한
        List<Comment> limitedReplies = replies.stream()
                .limit(DEFAULT_PAGE_SIZE + 1)
                .collect(Collectors.toList());

        boolean hasMore = limitedReplies.size() > DEFAULT_PAGE_SIZE;
        if (hasMore) {
            limitedReplies = limitedReplies.subList(0, DEFAULT_PAGE_SIZE);
        }

        // DTO 변환
        CommentResponseDto parentDto = CommentResponseDto.from(
            parentComment,
            commentRepository.countRepliesByParentId(commentId)
        );

        List<CommentResponseDto> replyDtos = limitedReplies.stream()
                .map(CommentResponseDto::fromReply)
                .collect(Collectors.toList());

        Long nextCursor = hasMore && !limitedReplies.isEmpty()
                ? limitedReplies.get(limitedReplies.size() - 1).getId()
                : null;

        return CommentThreadResponseDto.of(parentDto, replyDtos, nextCursor, hasMore);
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
