package com.onmoim.server.post.service;

import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;
import com.onmoim.server.post.dto.request.CursorPageRequestDto;
import com.onmoim.server.post.dto.response.CursorPageResponseDto;
import com.onmoim.server.post.dto.request.GroupPostRequestDto;
import com.onmoim.server.post.dto.response.GroupPostResponseDto;
import com.onmoim.server.group.entity.Group;
import com.onmoim.server.post.entity.GroupPost;
import com.onmoim.server.post.entity.GroupPostType;
import com.onmoim.server.post.repository.GroupPostQueryRepository;
import com.onmoim.server.post.repository.GroupPostRepository;
import com.onmoim.server.group.repository.GroupRepository;
import com.onmoim.server.user.entity.User;
import com.onmoim.server.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupPostService {

    private final GroupRepository groupRepository;
    private final GroupPostRepository groupPostRepository;
    private final GroupPostQueryRepository groupPostQueryRepository;
    private final UserRepository userRepository;

    /**
     * 그룹 조회 - 존재하지 않으면 예외 발생
     */
    private Group findGroupById(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_EXISTS_GROUP));
    }

    /**
     * 사용자 조회 - 존재하지 않으면 예외 발생
     */
    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_USER));
    }

    /**
     * 게시글 조회 - 존재하지 않으면 예외 발생
     */
    private GroupPost findPostById(Long postId) {
        return groupPostRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
    }

    /**
     * 게시글이 해당 그룹에 속하는지 확인
     */
    private void validatePostBelongsToGroup(GroupPost post, Long groupId) {
        if (!post.getGroup().getId().equals(groupId)) {
            throw new CustomException(ErrorCode.POST_NOT_FOUND);
        }
    }

    /**
     * 사용자가 게시글 작성자인지 확인
     */
    private void validatePostAuthor(GroupPost post, Long userId) {
        if (!post.getAuthor().getId().equals(userId)) {
            throw new CustomException(ErrorCode.DENIED_UNAUTHORIZED_USER);
        }
    }

    @Transactional
    public GroupPostResponseDto createPost(Long groupId, Long userId, GroupPostRequestDto request) {
        Group group = findGroupById(groupId);
        User user = findUserById(userId);

        // TODO: group 내 멤버 인지 확인하는 로직

        GroupPost post = GroupPost.builder()
                .group(group)
                .author(user)
                .title(request.getTitle())
                .content(request.getContent())
                .type(request.getType())
                .build();

        groupPostRepository.save(post);

        return GroupPostResponseDto.fromEntity(post);
    }

    public GroupPostResponseDto getPost(Long groupId, Long postId) {
        findGroupById(groupId);
        GroupPost post = findPostById(postId);
        validatePostBelongsToGroup(post, groupId);

        return GroupPostResponseDto.fromEntity(post);
    }

    /**
     * 커서 기반 페이징을 이용한 게시글 목록 조회
     */
    public CursorPageResponseDto<GroupPostResponseDto> getPostsWithCursor(Long groupId, GroupPostType type, CursorPageRequestDto request) {
        Group group = findGroupById(groupId);

        CursorPageResponseDto<GroupPost> postsPage = groupPostQueryRepository.findPostsWithCursor(
                group, type, request.getCursorId(), request.getSize());

        List<GroupPostResponseDto> content = postsPage.getContent()
                .stream()
                .map(GroupPostResponseDto::fromEntity)
                .collect(Collectors.toList());

        return CursorPageResponseDto.<GroupPostResponseDto>builder()
                .content(content)
                .hasNext(postsPage.isHasNext())
                .nextCursorId(postsPage.getNextCursorId())
                .build();
    }

    @Transactional
    public GroupPostResponseDto updatePost(Long groupId, Long postId, Long userId, GroupPostRequestDto request) {
        findGroupById(groupId);
        findUserById(userId);

        GroupPost post = findPostById(postId);
        validatePostBelongsToGroup(post, groupId);
        validatePostAuthor(post, userId);

        post.update(request.getTitle(), request.getContent(), request.getType());

        return GroupPostResponseDto.fromEntity(post);
    }

    @Transactional
    public void deletePost(Long groupId, Long postId, Long userId) {
        findGroupById(groupId);
        findUserById(userId);

        GroupPost post = findPostById(postId);
        validatePostBelongsToGroup(post, groupId);
        validatePostAuthor(post, userId);

        groupPostRepository.delete(post);
    }
}
