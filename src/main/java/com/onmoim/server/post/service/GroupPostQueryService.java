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

    /**
     * 게시글 조회 - 존재하지 않으면 예외 발생
     */
    public GroupPost findById(Long postId) {
        return groupPostRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
    }

    /**
     * 게시글이 해당 그룹에 속하는지 확인
     */
    public void validatePostBelongsToGroup(GroupPost post, Long groupId) {
        if (!post.getGroup().getId().equals(groupId)) {
            throw new CustomException(ErrorCode.POST_NOT_FOUND);
        }
    }

    /**
     * 사용자가 게시글 작성자인지 확인
     */
    public void validatePostAuthor(GroupPost post, Long userId) {
        if (!post.getAuthor().getId().equals(userId)) {
            throw new CustomException(ErrorCode.DENIED_UNAUTHORIZED_USER);
        }
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
     * 게시글 저장
     */
    @Transactional
    public GroupPost save(GroupPost post) {
        return groupPostRepository.save(post);
    }

    /**
     * 단일 게시글 상세 조회
     */
    public GroupPostResponseDto getPost(Long groupId, Long postId) {
        groupQueryService.getById(groupId);
        GroupPost post = findById(postId);
        validatePostBelongsToGroup(post, groupId);

        return GroupPostResponseDto.fromEntity(post);
    }

    /**
     * 커서 기반 페이징을 이용한 게시글 목록 조회 (N+1 문제 해결)
     */
    public CursorPageResponseDto<GroupPostResponseDto> getPosts(
            Long groupId,
            GroupPostType type,
            CursorPageRequestDto request
    ) {
        Group group = groupQueryService.getById(groupId);

        // 최적화된 메서드 사용 - 게시글과 이미지를 함께 조회
        return groupPostRepository.findPostsWithImages(
                group,
                type,
                request.getCursorId(),
                request.getSize()
        );
    }
}
