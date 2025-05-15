package com.onmoim.server.post.controller;

import com.onmoim.server.post.dto.request.CursorPageRequestDto;
import com.onmoim.server.post.dto.response.CursorPageResponseDto;
import com.onmoim.server.post.dto.request.GroupPostRequestDto;
import com.onmoim.server.post.dto.response.GroupPostResponseDto;
import com.onmoim.server.post.entity.GroupPostType;
import com.onmoim.server.post.service.GroupPostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 모임 게시글 관련 API 컨트롤러
 *
 * TODO: 인증 구현 후 @RequestParam userId 대신 인증 객체 사용하도록 변경
 * TODO: 모임 멤버 여부 검증 로직 추가
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class GroupPostController {

    private final GroupPostService groupPostService;

    /**
     * 모임 게시글 작성
     */
    @PostMapping("/v1/groups/{groupId}/posts")
    public ResponseEntity<GroupPostResponseDto> createPost(
            @PathVariable Long groupId,
            @Valid @RequestBody GroupPostRequestDto request,
            @RequestParam Long userId) {
        // TODO: 모임 멤버인지 확인하는 검증 로직 추가
        GroupPostResponseDto response = groupPostService.createPost(groupId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 모임 게시글 상세 조회
     *
     */
    @GetMapping("/v1/groups/{groupId}/posts/{postId}")
    public ResponseEntity<GroupPostResponseDto> getPost(
            @PathVariable Long groupId,
            @PathVariable Long postId) {
        GroupPostResponseDto response = groupPostService.getPost(groupId, postId);
        return ResponseEntity.ok(response);
    }

    /**
     * 모임 게시글 목록 조회 (커서 기반 페이징 무한 스크롤)
     *
     */
    @GetMapping("/v1/groups/{groupId}/posts")
    public ResponseEntity<CursorPageResponseDto<GroupPostResponseDto>> getPostsWithCursor(
            @PathVariable Long groupId,
            @RequestParam(required = false) GroupPostType type,
            @RequestParam(required = false) Long cursorId,
            @RequestParam(defaultValue = "10") int size) {

        CursorPageRequestDto cursorRequest = CursorPageRequestDto.builder()
                .cursorId(cursorId)
                .size(size)
                .build();

        CursorPageResponseDto<GroupPostResponseDto> response =
                groupPostService.getPostsWithCursor(groupId, type, cursorRequest);

        return ResponseEntity.ok(response);
    }

    /**
     * 모임 게시글 수정
     *
     */
    @PutMapping("/v1/groups/{groupId}/posts/{postId}")
    public ResponseEntity<GroupPostResponseDto> updatePost(
            @PathVariable Long groupId,
            @PathVariable Long postId,
            @Valid @RequestBody GroupPostRequestDto request,
            @RequestParam Long userId) {
        // TODO: 게시글 작성자 또는 모임장인지 확인하는 검증 로직 추가
        GroupPostResponseDto response = groupPostService.updatePost(groupId, postId, userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 모임 게시글 삭제
     */
    @DeleteMapping("/v1/groups/{groupId}/posts/{postId}")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long groupId,
            @PathVariable Long postId,
            @RequestParam Long userId) {
        // TODO: 게시글 작성자 또는 모임장인지 확인하는 검증 로직 추가
        groupPostService.deletePost(groupId, postId, userId);
        return ResponseEntity.noContent().build();
    }

}
