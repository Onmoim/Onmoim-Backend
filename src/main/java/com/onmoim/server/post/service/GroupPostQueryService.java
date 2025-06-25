package com.onmoim.server.post.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;
import com.onmoim.server.group.entity.Group;
import com.onmoim.server.group.entity.Status;
import com.onmoim.server.group.implement.GroupQueryService;
import com.onmoim.server.group.implement.GroupUserQueryService;
import com.onmoim.server.post.dto.request.CursorPageRequestDto;
import com.onmoim.server.post.dto.response.CursorPageResponseDto;
import com.onmoim.server.post.dto.response.GroupPostResponseDto;
import com.onmoim.server.post.entity.GroupPost;
import com.onmoim.server.post.entity.GroupPostType;
import com.onmoim.server.post.repository.GroupPostRepository;
import com.onmoim.server.post.util.PostValidationUtils;
import com.onmoim.server.post.vo.PostLikeInfo;

/**
 * 모임 게시글 조회용 Service
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GroupPostQueryService {

    private final GroupPostRepository groupPostRepository;
    private final GroupQueryService groupQueryService;
    private final GroupUserQueryService groupUserQueryService;
    private final PostLikeQueryService postLikeQueryService;

    /**
     * 게시글 조회
     */
    public GroupPost findById(Long postId) {
        GroupPost post = groupPostRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        PostValidationUtils.validatePostNotDeleted(post);
        return post;
    }

    /**
     * 사용자가 그룹의 멤버인지 확인
     * MEMBER 또는 OWNER만 게시글 관련 작업 가능
     */
    public void validateGroupMembership(Long groupId, Long userId) {
        groupUserQueryService.findById(groupId, userId)
                .filter(gu -> gu.getStatus() == Status.MEMBER || gu.getStatus() == Status.OWNER)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_GROUP_MEMBER));
    }

    /**
     * 게시글 접근 권한 통합 검증 (댓글 작성/답글 작성용)
     * - 게시글 존재 확인
     * - 게시글이 해당 그룹에 속하는지 확인
     * - 사용자가 그룹 멤버인지 확인
     */
    public GroupPost validatePostAccess(Long postId, Long groupId, Long userId) {
        GroupPost post = findById(postId);
        post.validateBelongsToGroup(groupId);
        validateGroupMembership(groupId, userId);
        return post;
    }

    /**
     * 게시글 조회 권한 검증 (댓글 목록 조회용)
     * - 게시글 존재 확인
     * - 게시글이 해당 그룹에 속하는지 확인
     */
    public GroupPost validatePostReadAccess(Long postId, Long groupId) {
        GroupPost post = findById(postId);
        post.validateBelongsToGroup(groupId);
        return post;
    }

    /**
     * 게시글 저장
     */
    @Transactional
    public GroupPost save(GroupPost post) {
        return groupPostRepository.save(post);
    }

    /**
     * 단일 게시글 상세 조회
     */
    public GroupPostResponseDto getPost(Long groupId, Long postId, Long userId) {
        groupQueryService.getById(groupId);
        GroupPost post = findById(postId);
        post.validateBelongsToGroup(groupId);

        PostLikeInfo likeInfo = postLikeQueryService.getPostLikeInfo(postId, userId);

        return GroupPostResponseDto.fromEntityWithLikes(
                post,
                likeInfo.likeCount(),
                likeInfo.isLiked()
        );
    }

    /**
     * 커서 기반 페이징을 이용한 게시글 목록 조회
     */
    public CursorPageResponseDto<GroupPostResponseDto> getPosts(
            Long groupId,
            GroupPostType type,
            CursorPageRequestDto request,
            Long userId
    ) {
        Group group = groupQueryService.getById(groupId);

        return groupPostRepository.findPostsWithImagesAndLikes(
                group,
                type,
                request.getCursorId(),
                request.getSize(),
                userId
        );
    }
}
