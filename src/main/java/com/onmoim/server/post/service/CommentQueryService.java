package com.onmoim.server.post.service;

import static com.onmoim.server.post.constant.PostConstants.CURSOR_PAGE_SIZE;

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
import com.onmoim.server.post.repository.CommentRepository.CommentReplyCountProjection;
import com.onmoim.server.post.util.PostValidationUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentQueryService {

	private final CommentRepository commentRepository;

	public CursorPageResponseDto<CommentResponseDto> getParentComments(GroupPost post, Long cursor) {
		var all = commentRepository.findParentCommentsByPost(post, cursor);
		var pagePlusOne = all.stream()
			.limit(CURSOR_PAGE_SIZE + 1)
			.toList();
		var hasNext = pagePlusOne.size() > CURSOR_PAGE_SIZE;
		var page = pagePlusOne.subList(0, Math.min(pagePlusOne.size(), CURSOR_PAGE_SIZE));

		List<Long> parentIds = page.stream()
			.map(Comment::getId)
			.toList();
		Map<Long, Long> replyCounts = fetchReplyCounts(parentIds);

		var dtos = page.stream()
			.map(c -> CommentResponseDto.from(c, replyCounts.getOrDefault(c.getId(), 0L)))
			.toList();

		var nextCursor = calculateNextCursor(hasNext, page);

		return CursorPageResponseDto.<CommentResponseDto>builder()
			.content(dtos)
			.nextCursorId(nextCursor)
			.hasNext(hasNext)
			.build();
	}

	public CommentThreadResponseDto getCommentThread(Long commentId, Long cursor) {
		var parent = commentRepository.findByIdWithAuthor(commentId)
			.orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));
		PostValidationUtils.validateCommentNotDeleted(parent);
		PostValidationUtils.validateParentComment(parent);

		var repliesAll = commentRepository.findRepliesByParent(parent, cursor);
		var repliesPlusOne = repliesAll.stream()
			.limit(CURSOR_PAGE_SIZE + 1)
			.toList();
		var hasNext = repliesPlusOne.size() > CURSOR_PAGE_SIZE;
		var replies = repliesPlusOne.subList(0, Math.min(repliesPlusOne.size(), CURSOR_PAGE_SIZE));

		Map<Long, Long> replyCounts = fetchReplyCounts(List.of(parent.getId()));

		var parentDto = CommentResponseDto.from(parent, replyCounts.getOrDefault(parent.getId(), 0L));
		var replyDtos = replies.stream()
			.map(CommentResponseDto::fromReply)
			.toList();

		var nextCursor = calculateNextCursor(hasNext, replies);

		return CommentThreadResponseDto.of(parentDto, replyDtos, nextCursor, hasNext);
	}

	private Map<Long, Long> fetchReplyCounts(List<Long> parentIds) {
		if (parentIds.isEmpty()) {
			return Map.of();
		}
		return commentRepository.countRepliesByParentIds(parentIds)
			.stream()
			.collect(Collectors.toUnmodifiableMap(
				CommentReplyCountProjection::getParentId,
				CommentReplyCountProjection::getReplyCount
			));
	}

	private Long calculateNextCursor(boolean hasNext, List<Comment> comments) {
		if (!hasNext || comments.isEmpty()) {
			return null;
		}
		return comments.stream()
			.map(Comment::getId)
			.reduce((first, second) -> second)
			.orElseThrow(() -> new IllegalStateException("댓글 리스트가 비어 있습니다."));
	}
}
