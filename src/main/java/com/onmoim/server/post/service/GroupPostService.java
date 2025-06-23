package com.onmoim.server.post.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

import com.onmoim.server.post.dto.request.CursorPageRequestDto;
import com.onmoim.server.post.dto.request.GroupPostRequestDto;
import com.onmoim.server.post.dto.response.CursorPageResponseDto;
import com.onmoim.server.post.dto.response.GroupPostResponseDto;
import com.onmoim.server.post.entity.GroupPostType;
import com.onmoim.server.post.entity.GroupPost;
import com.onmoim.server.user.entity.User;
import com.onmoim.server.user.service.UserQueryService;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupPostService {

    private final GroupPostQueryService groupPostQueryService;
    private final GroupPostCommandService groupPostCommandService;
    private final PostLikeService postLikeService;
    private final UserQueryService userQueryService;

    /**
     * 모임 게시글 작성
     */
    @Transactional
    public GroupPostResponseDto createPost(
            Long groupId,
            Long userId,
            GroupPostRequestDto request,
            List<MultipartFile> files
    ) {
        return groupPostCommandService.createPost(
                groupId,
                userId,
                request,
                files
        );
    }


    /**
     * 모임 게시글 조회 (좋아요 정보 포함)
     */
    public GroupPostResponseDto getPostWithLikes(
            Long groupId,
            Long postId,
            Long userId
    ) {
        return groupPostQueryService.getPostWithLikes(groupId, postId, userId);
    }

    /**
     * 커서 기반 페이징을 이용한 게시글 목록 조회
     */
    public CursorPageResponseDto<GroupPostResponseDto> getPostsWithLikes(
            Long groupId,
            GroupPostType type,
            CursorPageRequestDto request,
            Long userId
    ) {
        return groupPostQueryService.getPostsWithLikes(groupId, type, request, userId);
    }

    /**
     * 모임 게시글 수정
     */
    @Transactional
    public GroupPostResponseDto updatePost(
            Long groupId,
            Long postId,
            Long userId,
            GroupPostRequestDto request,
            List<MultipartFile> files
    ) {
        return groupPostCommandService.updatePost(
                groupId,
                postId,
                userId,
                request,
                files
        );
    }

    /**
     * 모임 게시글 삭제
     */
    @Transactional
    public void deletePost(
            Long groupId,
            Long postId,
            Long userId
    ) {
        groupPostCommandService.deletePost(groupId, postId, userId);
    }

    /**
     * 게시글 좋아요 토글
     */
    @Transactional
    public boolean togglePostLike(
            Long groupId,
            Long postId,
            Long userId
    ) {
        GroupPost post = groupPostQueryService.findById(postId);
        groupPostQueryService.validatePostBelongsToGroup(post, groupId);
        groupPostQueryService.validateGroupMembership(groupId, userId);

        User user = userQueryService.findById(userId);
        return postLikeService.toggleLike(post, user);
    }
}
