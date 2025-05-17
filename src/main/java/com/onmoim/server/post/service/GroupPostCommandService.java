package com.onmoim.server.post.service;

import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;
import com.onmoim.server.common.image.entity.Image;
import com.onmoim.server.common.s3.dto.FileUploadResponseDto;
import com.onmoim.server.common.s3.service.FileStorageService;
import com.onmoim.server.post.dto.request.GroupPostRequestDto;
import com.onmoim.server.post.dto.response.GroupPostResponseDto;
import com.onmoim.server.group.entity.Group;
import com.onmoim.server.post.entity.GroupPost;
import com.onmoim.server.post.entity.PostImage;
import com.onmoim.server.group.service.GroupQueryService;
import com.onmoim.server.user.entity.User;
import com.onmoim.server.user.service.UserQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class GroupPostCommandService {

	private final GroupQueryService groupQueryService;
	private final GroupPostQueryService groupPostQueryService;
	private final UserQueryService userQueryService;
	private final ImagePostService imagePostService;
	private final FileStorageService fileStorageService;

	// 게시글당 최대 이미지 개수
	private static final int MAX_IMAGES_PER_POST = 5;

	/**
	 * 이미지 개수 유효성 검증
	 */
	private List<MultipartFile> validateAndFilterImages(List<MultipartFile> files) {

		if (CollectionUtils.isEmpty(files)) {
			return new ArrayList<>();
		}

		List<MultipartFile> validFiles = files.stream()
			.filter(file -> !file.isEmpty())
			.toList();

		if (validFiles.size() > MAX_IMAGES_PER_POST) {
			throw new CustomException(ErrorCode.IMAGE_COUNT_EXCEEDED);
		}

		return validFiles;
	}

	/**
	 * 이미지 업로드 처리 (최대 5개까지)
	 */
	private List<PostImage> processImageUploads(GroupPost post, List<MultipartFile> files) {
		// 이미지 유효성 검증 및 필터링
		List<MultipartFile> validFiles = validateAndFilterImages(files);
		if (validFiles.isEmpty()) {
			return new ArrayList<>();
		}

		// S3에 파일 업로드 후 Image 엔티티 생성
		List<Image> images = new ArrayList<>();
		for (MultipartFile file : validFiles) {
			// S3에 파일 업로드
			FileUploadResponseDto uploadResult = fileStorageService.uploadFile(file, "posts");

			// Image 엔티티 생성
			Image image = Image.builder()
				.imageUrl(uploadResult.getFileUrl())
				.build();

			images.add(image);
		}

		// 이미지와 게시글 이미지 함께 일괄 저장
		return imagePostService.saveImages(images, post);
	}

	/**
	 * 모임 게시글 작성
	 */
	public GroupPostResponseDto createPost(Long groupId, Long userId, GroupPostRequestDto request, List<MultipartFile> files) {
		Group group = groupQueryService.getById(groupId);
		User user = userQueryService.findById(userId);

		// 그룹 멤버 확인
		groupPostQueryService.validateGroupMembership(groupId, userId);

		GroupPost post = GroupPost.builder()
			.group(group)
			.author(user)
			.title(request.getTitle())
			.content(request.getContent())
			.type(request.getType())
			.build();

		groupPostQueryService.save(post);

		// 이미지 업로드 처리 (파일이 있는 경우)
		if (files != null && !files.isEmpty()) {
			processImageUploads(post, files);
		}

		return GroupPostResponseDto.fromEntity(post);
	}

	/**
	 * 모임 게시글 수정
	 */
	public GroupPostResponseDto updatePost(Long groupId, Long postId, Long userId, GroupPostRequestDto request, List<MultipartFile> files) {
		// 그룹 및 멤버십 확인
		groupQueryService.getById(groupId);
		groupPostQueryService.validateGroupMembership(groupId, userId);

		GroupPost post = groupPostQueryService.findById(postId);
		groupPostQueryService.validatePostBelongsToGroup(post, groupId);
		groupPostQueryService.validatePostAuthor(post, userId);

		post.update(request.getTitle(), request.getContent(), request.getType());

		// 이미지 업데이트 처리 (파일이 있는 경우)
		if (!CollectionUtils.isEmpty(files)) {
			// 기존 이미지 일괄 소프트 삭제
			imagePostService.softDeleteAllByPostId(post.getId());

			// 새 파일 업로드 처리
			processImageUploads(post, files);
		}

		return GroupPostResponseDto.fromEntity(post);
	}

	/**
	 * 모임 게시글 삭제
	 */
	public void deletePost(Long groupId, Long postId, Long userId) {
		// 그룹 및 멤버십 확인
		groupQueryService.getById(groupId);
		groupPostQueryService.validateGroupMembership(groupId, userId);

		GroupPost post = groupPostQueryService.findById(postId);
		groupPostQueryService.validatePostBelongsToGroup(post, groupId);
		groupPostQueryService.validatePostAuthor(post, userId);

		// 소프트 삭제 처리 - 게시글
		post.softDelete();

		// 게시글에 첨부된 이미지도 일괄 소프트 삭제 처리
		imagePostService.softDeleteAllByPostId(post.getId());

		// TODO: 추후 스케줄링 및 배치 처리를 통해 실제 데이터와 S3 이미지 파일 삭제 구현
		// 1. 일정 기간(예: 30일) 이후 하드 삭제 처리
		// 2. 특정 시간대에 배치 작업으로 삭제 데이터 정리
	}
}
