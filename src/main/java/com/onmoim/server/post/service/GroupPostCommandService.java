package com.onmoim.server.post.service;

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

	/**
	 * 이미지 업로드 처리
	 */
	private List<PostImage> processImageUploads(GroupPost post, List<MultipartFile> files) {
		if (files == null || files.isEmpty()) {
			return new ArrayList<>();
		}

		List<PostImage> postImages = new ArrayList<>();

		for (MultipartFile file : files) {
			if (file.isEmpty()) {
				continue;
			}

			// S3에 파일 업로드
			FileUploadResponseDto uploadResult = fileStorageService.uploadFile(file, "posts");

			// Image 엔티티 생성
			Image image = Image.builder()
				.imageUrl(uploadResult.getFileUrl())
				.build();

			// 이미지와 게시글 이미지 함께 저장
			PostImage postImage = imagePostService.saveImageAndPostImage(image, post);
			post.addImage(postImage);
			postImages.add(postImage);
		}

		return postImages;
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
		if (files != null && !files.isEmpty()) {
			// 기존 파일 조회 (전체 이미지)
			List<PostImage> existingImages = imagePostService.findAllByPost(post);

			// 소프트 삭제 처리
			for (PostImage postImage : existingImages) {
				postImage.softDelete();
			}

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

		// 게시글에 첨부된 이미지도 소프트 삭제 처리
		List<PostImage> images = imagePostService.findAllByPost(post);
		for (PostImage postImage : images) {
			postImage.softDelete();
		}

		// TODO: 추후 스케줄링 및 배치 처리를 통해 실제 데이터와 S3 이미지 파일 삭제 구현
		// 1. 일정 기간(예: 30일) 이후 하드 삭제 처리
		// 2. 특정 시간대에 배치 작업으로 삭제 데이터 정리
	}
}
