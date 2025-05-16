package com.onmoim.server.post.repository;

import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;
import com.onmoim.server.group.entity.Group;
import com.onmoim.server.group.service.GroupQueryService;
import com.onmoim.server.post.dto.request.CursorPageRequestDto;
import com.onmoim.server.post.dto.response.CursorPageResponseDto;
import com.onmoim.server.post.dto.response.GroupPostResponseDto;
import com.onmoim.server.post.entity.GroupPost;
import com.onmoim.server.post.entity.GroupPostType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupPostQueryService {
    
    private final GroupPostRepository groupPostRepository;
    private final GroupQueryService groupQueryService;
    
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
     * 게시글 저장
     */
    @Transactional
    public GroupPost save(GroupPost post) {
        return groupPostRepository.save(post);
    }
    
    /**
     * 게시글 목록 조회 - 내부용
     */
    public CursorPageResponseDto<GroupPost> findPosts(Group group, GroupPostType type, Long cursorId, int size) {
        return groupPostRepository.findPosts(group, type, cursorId, size);
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
     * 커서 기반 페이징을 이용한 게시글 목록 조회
     */
    public CursorPageResponseDto<GroupPostResponseDto> getPosts(Long groupId, GroupPostType type, CursorPageRequestDto request) {
        Group group = groupQueryService.getById(groupId);

        CursorPageResponseDto<GroupPost> postsPage = findPosts(
            group, type, request.getCursorId(), request.getSize());

        List<GroupPostResponseDto> content = postsPage.getContent()
            .stream()
            .map(GroupPostResponseDto::fromEntity)
            .toList();

        return CursorPageResponseDto.<GroupPostResponseDto>builder()
            .content(content)
            .hasNext(postsPage.isHasNext())
            .nextCursorId(postsPage.getNextCursorId())
            .build();
    }
} 