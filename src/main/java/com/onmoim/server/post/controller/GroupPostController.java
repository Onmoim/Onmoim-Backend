package com.onmoim.server.post.controller;

import com.onmoim.server.post.dto.request.CursorPageRequestDto;
import com.onmoim.server.post.dto.response.CursorPageResponseDto;
import com.onmoim.server.post.dto.request.GroupPostRequestDto;
import com.onmoim.server.post.dto.response.GroupPostResponseDto;
import com.onmoim.server.post.entity.GroupPostType;
import com.onmoim.server.post.service.GroupPostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 모임 게시글 관련 API 컨트롤러
 *
 * TODO: 인증 구현 후 @RequestParam userId 대신 인증 객체 사용하도록 변경
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "모임 게시글 API", description = "모임 게시글 생성, 조회, 수정, 삭제 API")
public class GroupPostController {

    private final GroupPostService groupPostService;

    /**
     * 모임 게시글 작성
     */
    @Operation(summary = "모임 게시글 작성", description = "새로운 모임 게시글을 작성합니다. 이미지는 최대 5개까지 첨부 가능합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "게시글 작성 성공", 
                content = @Content(schema = @Schema(implementation = GroupPostResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "403", description = "모임 멤버가 아님"),
        @ApiResponse(responseCode = "404", description = "모임 또는 사용자를 찾을 수 없음")
    })
    @PostMapping(value = "/v1/groups/{groupId}/posts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GroupPostResponseDto> createPost(
            @Parameter(description = "모임 ID") @PathVariable Long groupId,
            @Parameter(description = "게시글 정보") @Valid @RequestPart(value = "request") GroupPostRequestDto request,
            @Parameter(description = "첨부 이미지 파일 (최대 5개)") @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @Parameter(description = "사용자 ID") @RequestParam Long userId) {
        GroupPostResponseDto response = groupPostService.createPost(groupId, userId, request, files);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 모임 게시글 상세 조회
     */
    @Operation(summary = "모임 게시글 상세 조회", description = "모임 게시글의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "게시글 조회 성공",
                content = @Content(schema = @Schema(implementation = GroupPostResponseDto.class))),
        @ApiResponse(responseCode = "404", description = "게시글 또는 모임을 찾을 수 없음")
    })
    @GetMapping("/v1/groups/{groupId}/posts/{postId}")
    public ResponseEntity<GroupPostResponseDto> getPost(
            @Parameter(description = "모임 ID") @PathVariable Long groupId,
            @Parameter(description = "게시글 ID") @PathVariable Long postId) {
        GroupPostResponseDto response = groupPostService.getPost(groupId, postId);
        return ResponseEntity.ok(response);
    }

    /**
     * 모임 게시글 목록 조회 (커서 기반 페이징 무한 스크롤)
     */
    @Operation(summary = "모임 게시글 목록 조회", description = "모임의 게시글 목록을 커서 기반 페이징으로 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "게시글 목록 조회 성공",
                content = @Content(schema = @Schema(implementation = CursorPageResponseDto.class))),
        @ApiResponse(responseCode = "404", description = "모임을 찾을 수 없음")
    })
    @GetMapping("/v1/groups/{groupId}/posts")
    public ResponseEntity<CursorPageResponseDto<GroupPostResponseDto>> getPosts(
            @Parameter(description = "모임 ID") @PathVariable Long groupId,
            @Parameter(description = "게시글 타입") @RequestParam(required = false) GroupPostType type,
            @Parameter(description = "커서 ID (마지막으로 조회한 게시글 ID)") @RequestParam(required = false) Long cursorId,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size) {

        CursorPageRequestDto cursorRequest = CursorPageRequestDto.builder()
                .cursorId(cursorId)
                .size(size)
                .build();

        CursorPageResponseDto<GroupPostResponseDto> response =
                groupPostService.getPosts(groupId, type, cursorRequest);

        return ResponseEntity.ok(response);
    }

    /**
     * 모임 게시글 수정
     */
    @Operation(summary = "모임 게시글 수정", description = "모임 게시글의 내용과 이미지를 수정합니다. 이미지를 첨부하면 기존 이미지는 모두 삭제되고 새로운 이미지로 대체됩니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "게시글 수정 성공",
                content = @Content(schema = @Schema(implementation = GroupPostResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "403", description = "게시글 수정 권한 없음"),
        @ApiResponse(responseCode = "404", description = "게시글 또는 모임을 찾을 수 없음")
    })
    @PutMapping(value = "/v1/groups/{groupId}/posts/{postId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GroupPostResponseDto> updatePost(
            @Parameter(description = "모임 ID") @PathVariable Long groupId,
            @Parameter(description = "게시글 ID") @PathVariable Long postId,
            @Parameter(description = "게시글 수정 정보") @Valid @RequestPart(value = "request") GroupPostRequestDto request,
            @Parameter(description = "첨부 이미지 파일 (최대 5개)") @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @Parameter(description = "사용자 ID") @RequestParam Long userId) {
        GroupPostResponseDto response = groupPostService.updatePost(groupId, postId, userId, request, files);
        return ResponseEntity.ok(response);
    }

    /**
     * 모임 게시글 삭제
     */
    @Operation(summary = "모임 게시글 삭제", description = "모임 게시글을 삭제합니다. 게시글 작성자만 삭제할 수 있습니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "게시글 삭제 성공"),
        @ApiResponse(responseCode = "403", description = "게시글 삭제 권한 없음"),
        @ApiResponse(responseCode = "404", description = "게시글 또는 모임을 찾을 수 없음")
    })
    @DeleteMapping("/v1/groups/{groupId}/posts/{postId}")
    public ResponseEntity<Void> deletePost(
            @Parameter(description = "모임 ID") @PathVariable Long groupId,
            @Parameter(description = "게시글 ID") @PathVariable Long postId,
            @Parameter(description = "사용자 ID") @RequestParam Long userId) {
        groupPostService.deletePost(groupId, postId, userId);
        return ResponseEntity.noContent().build();
    }
}
