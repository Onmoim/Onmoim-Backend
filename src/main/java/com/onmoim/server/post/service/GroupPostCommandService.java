package com.onmoim.server.post.service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;
import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;
import com.onmoim.server.common.s3.dto.FileUploadResponseDto;
import com.onmoim.server.common.s3.service.FileStorageService;
import com.onmoim.server.group.entity.Group;
import com.onmoim.server.group.implement.GroupQueryService;
import com.onmoim.server.post.constant.PostConstants;
import com.onmoim.server.post.dto.request.GroupPostRequestDto;
import com.onmoim.server.post.dto.response.GroupPostResponseDto;
import com.onmoim.server.post.entity.GroupPost;
import com.onmoim.server.post.entity.Image;
import com.onmoim.server.post.entity.PostImage;
import com.onmoim.server.user.entity.User;
import com.onmoim.server.user.service.UserQueryService;

@Service
@RequiredArgsConstructor
@Transactional
public class GroupPostCommandService {

    private final GroupQueryService groupQueryService;
    private final GroupPostQueryService groupPostQueryService;
    private final UserQueryService userQueryService;
    private final ImagePostService imagePostService;
    private final FileStorageService fileStorageService;

    private List<MultipartFile> validateAndFilterImages(List<MultipartFile> files) {
        if (CollectionUtils.isEmpty(files)) {
            return new ArrayList<>();
        }

        List<MultipartFile> validFiles = files.stream()
                .filter(file -> !file.isEmpty())
                .toList();

        if (validFiles.size() > PostConstants.MAX_IMAGES_PER_POST) {
            throw new CustomException(ErrorCode.IMAGE_COUNT_EXCEEDED);
        }

        return validFiles;
    }


    private Image uploadSingleFile(MultipartFile file) {
        FileUploadResponseDto uploadResult = fileStorageService.uploadFile(
                file,
                PostConstants.IMAGE_UPLOAD_PATH
        );

        return Image.builder()
                .imageUrl(uploadResult.getFileUrl())
                .build();
    }


    private List<Image> uploadMultipleFiles(List<MultipartFile> files) {
        return files.stream()
                .map(this::uploadSingleFile)
                .toList();
    }

    /**
     * 이미지 업로드 처리 (최대 5개까지)
     */
    private List<PostImage> processImageUploads(GroupPost post, List<MultipartFile> files) {
        List<MultipartFile> validFiles = validateAndFilterImages(files);
        if (validFiles.isEmpty()) {
            return new ArrayList<>();
        }

        List<Image> images = uploadMultipleFiles(validFiles);
        return imagePostService.saveImages(images, post);
    }

    /**
     * 모임 게시글 작성
     */
    public GroupPostResponseDto createPost(
            Long groupId,
            Long userId,
            GroupPostRequestDto request,
            List<MultipartFile> files
    ) {
        Group group = groupQueryService.getById(groupId);
        User user = userQueryService.findById(userId);
        groupPostQueryService.validateGroupMembership(groupId, userId);

        GroupPost post = GroupPost.builder()
                .group(group)
                .author(user)
                .title(request.getTitle())
                .content(request.getContent())
                .type(request.getType())
                .build();
        groupPostQueryService.save(post);

        List<PostImage> postImages = new ArrayList<>();
        if (files != null && !files.isEmpty()) {
            postImages = processImageUploads(post, files);
        }

        return GroupPostResponseDto.fromEntityWithImages(post, postImages);
    }

    /**
     * 모임 게시글 수정
     */
    public GroupPostResponseDto updatePost(
            Long groupId,
            Long postId,
            Long userId,
            GroupPostRequestDto request,
            List<MultipartFile> files
    ) {
        groupPostQueryService.validateGroupMembership(groupId, userId);

        GroupPost post = groupPostQueryService.findById(postId);
        post.validateBelongsToGroup(groupId);
        post.validateAuthor(userId);

        post.update(
                request.getTitle(),
                request.getContent(),
                request.getType()
        );

        List<PostImage> postImages = imagePostService.findAllByPost(post);
        if (!CollectionUtils.isEmpty(files)) {
            imagePostService.softDeleteAllByPostId(post.getId());
            postImages = processImageUploads(post, files);
        }

        return GroupPostResponseDto.fromEntityWithImages(post, postImages);
    }

    /**
     * 모임 게시글 삭제
     */
    public void deletePost(
            Long groupId,
            Long postId,
            Long userId
    ) {
        groupPostQueryService.validateGroupMembership(groupId, userId);

        GroupPost post = groupPostQueryService.findById(postId);
        post.validateBelongsToGroup(groupId);
        post.validateAuthor(userId);

        post.softDelete();
        imagePostService.softDeleteAllByPostId(post.getId());
    }
}
