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

    @Transactional
    public GroupPostResponseDto createPost(Long groupId, Long userId, GroupPostRequestDto request) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_EXISTS_GROUP));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_USER));

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
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_EXISTS_GROUP));

        GroupPost post = groupPostRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        if (!post.getGroup().getId().equals(groupId)) {
            throw new CustomException(ErrorCode.POST_NOT_FOUND);
        }

        return GroupPostResponseDto.fromEntity(post);
    }

    /**
     * 커서 기반 페이징을 이용한 게시글 목록 조회
     */
    public CursorPageResponseDto<GroupPostResponseDto> getPostsWithCursor(Long groupId, GroupPostType type, CursorPageRequestDto request) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_EXISTS_GROUP));

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
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_EXISTS_GROUP));

        GroupPost post = groupPostRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        if (!post.getGroup().getId().equals(groupId)) {
            throw new CustomException(ErrorCode.POST_NOT_FOUND);
        }

        if (!post.getAuthor().getId().equals(userId)) {
            throw new CustomException(ErrorCode.DENIED_UNAUTHORIZED_USER);
        }

        post.update(request.getTitle(), request.getContent(), request.getType());

        return GroupPostResponseDto.fromEntity(post);
    }

    @Transactional
    public void deletePost(Long groupId, Long postId, Long userId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_EXISTS_GROUP));

        GroupPost post = groupPostRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        if (!post.getGroup().getId().equals(groupId)) {
            throw new CustomException(ErrorCode.POST_NOT_FOUND);
        }

        if (!post.getAuthor().getId().equals(userId)) {
            throw new CustomException(ErrorCode.DENIED_UNAUTHORIZED_USER);
        }

        groupPostRepository.delete(post);
    }
}
