package com.onmoim.server.post.service;

import com.onmoim.server.post.dto.request.CursorPageRequestDto;
import com.onmoim.server.post.dto.response.CursorPageResponseDto;
import com.onmoim.server.post.dto.request.GroupPostRequestDto;
import com.onmoim.server.post.dto.response.GroupPostResponseDto;
import com.onmoim.server.post.entity.GroupPostType;
import com.onmoim.server.post.repository.GroupPostQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * GroupPostService는 이제 직접 비즈니스 로직을 처리하지 않고
 * GroupPostCommandService와 GroupPostQueryService에 위임합니다.
 * (Facade 역할)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupPostService {

	private final GroupPostQueryService groupPostQueryService;
	private final GroupPostCommandService groupPostCommandService;

	/**
	 * 모임 게시글 작성 - 파일 첨부 가능
	 */
	@Transactional
	public GroupPostResponseDto createPost(Long groupId, Long userId, GroupPostRequestDto request, List<MultipartFile> files) {
		return groupPostCommandService.createPost(groupId, userId, request, files);
	}

	/**
	 * 모임 게시글 조회
	 */
	public GroupPostResponseDto getPost(Long groupId, Long postId) {
		return groupPostQueryService.getPost(groupId, postId);
	}

	/**
	 * 커서 기반 페이징을 이용한 게시글 목록 조회
	 */
	public CursorPageResponseDto<GroupPostResponseDto> getPosts(Long groupId, GroupPostType type, CursorPageRequestDto request) {
		return groupPostQueryService.getPosts(groupId, type, request);
	}

	/**
	 * 모임 게시글 수정 - 파일 첨부 가능
	 */
	@Transactional
	public GroupPostResponseDto updatePost(Long groupId, Long postId, Long userId, GroupPostRequestDto request, List<MultipartFile> files) {
		return groupPostCommandService.updatePost(groupId, postId, userId, request, files);
	}

	/**
	 * 모임 게시글 삭제
	 */
	@Transactional
	public void deletePost(Long groupId, Long postId, Long userId) {
		groupPostCommandService.deletePost(groupId, postId, userId);
	}
} 