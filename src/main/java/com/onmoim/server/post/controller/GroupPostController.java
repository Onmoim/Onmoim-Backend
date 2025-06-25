package com.onmoim.server.post.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

import com.onmoim.server.common.response.ResponseHandler;
import com.onmoim.server.post.dto.request.CursorPageRequestDto;
import com.onmoim.server.post.dto.request.GroupPostRequestDto;
import com.onmoim.server.post.dto.response.CursorPageResponseDto;
import com.onmoim.server.post.dto.response.GroupPostResponseDto;
import com.onmoim.server.post.dto.response.PostLikeToggleResponseDto;

import com.onmoim.server.post.entity.GroupPostType;
import com.onmoim.server.post.service.GroupPostService;
import com.onmoim.server.post.service.PostLikeService;
import com.onmoim.server.post.service.CommentService;
import com.onmoim.server.post.service.CommentQueryService;
import com.onmoim.server.post.service.GroupPostQueryService;
import com.onmoim.server.post.dto.request.CommentRequestDto;
import com.onmoim.server.post.dto.response.CommentResponseDto;
import com.onmoim.server.post.dto.response.CommentThreadResponseDto;
import com.onmoim.server.post.entity.GroupPost;
import com.onmoim.server.user.entity.User;
import com.onmoim.server.user.service.UserQueryService;
import com.onmoim.server.security.CustomUserDetails;

/**
 * 모임 게시글 관련 API 컨트롤러
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "모임 게시글 API", description = "모임 게시글 생성, 조회, 수정, 삭제 API")
public class GroupPostController {

    private final GroupPostService groupPostService;
    private final PostLikeService postLikeService;
    private final CommentService commentService;
    private final CommentQueryService commentQueryService;
    private final GroupPostQueryService groupPostQueryService;
    private final UserQueryService userQueryService;


    @Operation(
            summary = "모임 게시글 작성",
            description = "새로운 모임 게시글을 작성합니다. 이미지는 최대 5개까지 첨부 가능합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "게시글 작성 성공",
                    content = @Content(
                            schema = @Schema(implementation = ResponseHandler.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "400", description = "모임 멤버가 아님"),
            @ApiResponse(responseCode = "400", description = "모임 또는 사용자를 찾을 수 없음")
    })
    @PostMapping(
            value = "/groups/{groupId}/posts",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ResponseHandler<GroupPostResponseDto>> createPost(
            @Parameter(description = "모임 ID")
            @PathVariable Long groupId,
            @Parameter(description = "게시글 정보")
            @Valid @RequestPart(value = "request") GroupPostRequestDto request,
            @Parameter(description = "첨부 이미지 파일")
            @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) {
        Long userId = getCurrentUserId();
        GroupPostResponseDto response = groupPostService.createPost(
                groupId,
                userId,
                request,
                files
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ResponseHandler.response(response));
    }

    @Operation(
            summary = "모임 게시글 상세 조회",
            description = "모임 게시글의 상세 정보를 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "게시글 조회 성공",
                    content = @Content(
                            schema = @Schema(implementation = ResponseHandler.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "게시글 또는 모임을 찾을 수 없음")
    })
    @GetMapping("/groups/{groupId}/posts/{postId}")
    public ResponseEntity<ResponseHandler<GroupPostResponseDto>> getPost(
            @Parameter(description = "모임 ID")
            @PathVariable Long groupId,
            @Parameter(description = "게시글 ID")
            @PathVariable Long postId
    ) {
        Long userId = getCurrentUserIdOrNull();
        GroupPostResponseDto response =
                groupPostService.getPostWithLikes(groupId, postId, userId);
        return ResponseEntity.ok(ResponseHandler.response(response));
    }

    /**
     * 모임 게시글 목록 조회 (커서 기반 페이징 무한 스크롤)
     */
    @Operation(
            summary = "모임 게시글 목록 조회",
            description = "모임의 게시글 목록을 커서 기반 페이징으로 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "게시글 목록 조회 성공",
                    content = @Content(
                            schema = @Schema(implementation = ResponseHandler.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "모임을 찾을 수 없음")
    })
    @GetMapping("/groups/{groupId}/posts")
    public ResponseEntity<ResponseHandler<CursorPageResponseDto<GroupPostResponseDto>>> getPosts(
            @Parameter(description = "모임 ID")
            @PathVariable Long groupId,
            @Parameter(description = "게시글 타입")
            @RequestParam(required = false) GroupPostType type,
            @Parameter(description = "커서 ID (마지막으로 조회한 게시글 ID)")
            @RequestParam(required = false) Long cursorId,
            @Parameter(description = "페이지 크기")
            @RequestParam(defaultValue = "10") int size
    ) {
        CursorPageRequestDto cursorRequest =
                CursorPageRequestDto.builder()
                        .cursorId(cursorId)
                        .size(size)
                        .build();

        Long userId = getCurrentUserIdOrNull();
        CursorPageResponseDto<GroupPostResponseDto> response =
                groupPostService.getPostsWithLikes(
                        groupId,
                        type,
                        cursorRequest,
                        userId
                );

        return ResponseEntity.ok(ResponseHandler.response(response));
    }

    /**
     * 모임 게시글 수정
     */
    @Operation(
            summary = "모임 게시글 수정",
            description = "모임 게시글의 내용과 이미지를 수정합니다. " +
                    "이미지를 첨부하면 기존 이미지는 모두 삭제되고 새로운 이미지로 대체됩니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "게시글 수정 성공",
                    content = @Content(
                            schema = @Schema(implementation = ResponseHandler.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "400", description = "게시글 수정 권한 없음"),
            @ApiResponse(responseCode = "400", description = "게시글 또는 모임을 찾을 수 없음")
    })
    @PutMapping(
            value = "/groups/{groupId}/posts/{postId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ResponseHandler<GroupPostResponseDto>> updatePost(
            @Parameter(description = "모임 ID")
            @PathVariable Long groupId,
            @Parameter(description = "게시글 ID")
            @PathVariable Long postId,
            @Parameter(description = "게시글 수정 정보")
            @Valid @RequestPart(value = "request") GroupPostRequestDto request,
            @Parameter(description = "첨부 이미지 파일")
            @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) {
        Long userId = getCurrentUserId();
        GroupPostResponseDto response =
                groupPostService.updatePost(
                        groupId,
                        postId,
                        userId,
                        request,
                        files
                );
        return ResponseEntity.ok(ResponseHandler.response(response));
    }

    /**
     * 모임 게시글 삭제
     */
    @Operation(
            summary = "모임 게시글 삭제",
            description = "모임 게시글을 삭제합니다. 게시글 작성자만 삭제할 수 있습니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "게시글 삭제 성공"),
            @ApiResponse(responseCode = "400", description = "게시글 삭제 권한 없음"),
            @ApiResponse(responseCode = "400", description = "게시글 또는 모임을 찾을 수 없음")
    })
    @DeleteMapping("/groups/{groupId}/posts/{postId}")
    public ResponseEntity<ResponseHandler<Void>> deletePost(
            @Parameter(description = "모임 ID")
            @PathVariable Long groupId,
            @Parameter(description = "게시글 ID")
            @PathVariable Long postId
    ) {
        Long userId = getCurrentUserId();
        groupPostService.deletePost(groupId, postId, userId);
        return ResponseEntity.ok(ResponseHandler.response(null));
    }

	//좋아요 API

    /**
     * 게시글 좋아요 토글
     */
    @Operation(
            summary = "게시글 좋아요 토글",
            description = "게시글에 좋아요를 추가하거나 취소합니다. 좋아요 상태에 따라 자동으로 토글됩니다. 처음 좋아요 클릭시 좋아요"
	            + "두번 누르면 놓아요 취소" + "세번 누르면 다시 좋아요 활성화"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "좋아요 토글 성공"),
            @ApiResponse(responseCode = "400", description = "게시글 또는 모임을 찾을 수 없음"),
            @ApiResponse(responseCode = "401", description = "로그인이 필요합니다")
    })
    @PostMapping("/groups/{groupId}/posts/{postId}/like")
    public ResponseEntity<ResponseHandler<PostLikeToggleResponseDto>> togglePostLike(
            @Parameter(description = "모임 ID")
            @PathVariable Long groupId,
            @Parameter(description = "게시글 ID")
            @PathVariable Long postId
    ) {
        Long userId = getCurrentUserId();
        boolean isLiked = groupPostService.togglePostLike(groupId, postId, userId);

        PostLikeToggleResponseDto response = new PostLikeToggleResponseDto(isLiked);
        return ResponseEntity.ok(ResponseHandler.response(response));
    }



    //  댓글 관련 API

    /**
     * 게시글의 부모댓글 목록 조회
     */
    @Operation(
            summary = "게시글의 댓글 목록 조회",
            description = "게시글의 부모댓글 목록을 조회합니다. 답글 개수도 함께 제공됩니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "댓글 목록 조회 성공"),
            @ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음")
    })
    @GetMapping("/groups/{groupId}/posts/{postId}/comments")
    public ResponseEntity<ResponseHandler<CursorPageResponseDto<CommentResponseDto>>> getComments(
            @Parameter(description = "모임 ID")
            @PathVariable Long groupId,
            @Parameter(description = "게시글 ID")
            @PathVariable Long postId,
            @Parameter(description = "커서 ID (마지막으로 조회한 댓글 ID)")
            @RequestParam(required = false) Long cursor
    ) {

        GroupPost post = groupPostQueryService.validatePostReadAccess(postId, groupId);

        CursorPageResponseDto<CommentResponseDto> response =
                commentQueryService.getParentComments(post, cursor);
        return ResponseEntity.ok(ResponseHandler.response(response));
    }

    /**
     * 특정 댓글의 답글 목록 조회 (댓글 스레드)
     */
    @Operation(
            summary = "댓글의 답글 목록 조회",
            description = "특정 부모댓글과 그에 대한 답글 목록을 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "답글 목록 조회 성공"),
            @ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없음"),
            @ApiResponse(responseCode = "400", description = "부모댓글이 아님")
    })
    @GetMapping("/groups/{groupId}/posts/{postId}/comments/{commentId}/thread")
    public ResponseEntity<ResponseHandler<CommentThreadResponseDto>> getCommentThread(
            @Parameter(description = "모임 ID")
            @PathVariable Long groupId,
            @Parameter(description = "게시글 ID")
            @PathVariable Long postId,
            @Parameter(description = "댓글 ID")
            @PathVariable Long commentId,
            @Parameter(description = "커서 ID (마지막으로 조회한 답글 ID)")
            @RequestParam(required = false) Long cursor
    ) {
        CommentThreadResponseDto response =
                commentQueryService.getCommentThread(commentId, cursor);
        return ResponseEntity.ok(ResponseHandler.response(response));
    }

    /**
     * 부모댓글 작성
     */
    @Operation(
            summary = "댓글 작성",
            description = "게시글에 새로운 댓글을 작성합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "댓글 작성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음")
    })
    @PostMapping("/groups/{groupId}/posts/{postId}/comments")
    public ResponseEntity<ResponseHandler<Long>> createComment(
            @Parameter(description = "모임 ID")
            @PathVariable Long groupId,
            @Parameter(description = "게시글 ID")
            @PathVariable Long postId,
            @Parameter(description = "댓글 내용")
            @Valid @RequestPart CommentRequestDto request
    ) {
        Long userId = getCurrentUserId();

        GroupPost post = groupPostQueryService.validatePostAccess(postId, groupId, userId);

        User user = userQueryService.findById(userId);
        Long commentId = commentService.createParentComment(post, user, request.getContent());
        return ResponseEntity.status(HttpStatus.CREATED).body(ResponseHandler.response(commentId));
    }

    /**
     * 답글 작성
     */
    @Operation(
            summary = "답글 작성",
            description = "특정 댓글에 답글을 작성합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "답글 작성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없음")
    })
    @PostMapping("/groups/{groupId}/posts/{postId}/comments/{commentId}/replies")
    public ResponseEntity<ResponseHandler<Long>> createReply(
            @Parameter(description = "모임 ID")
            @PathVariable Long groupId,
            @Parameter(description = "게시글 ID")
            @PathVariable Long postId,
            @Parameter(description = "부모 댓글 ID")
            @PathVariable Long commentId,
            @Parameter(description = "답글 내용")
            @Valid @RequestPart CommentRequestDto request
    ) {
        Long userId = getCurrentUserId();

        GroupPost post = groupPostQueryService.validatePostAccess(postId, groupId, userId);

        User user = userQueryService.findById(userId);
        Long replyId = commentService.createReply(post, user, commentId, request.getContent());
        return ResponseEntity.status(HttpStatus.CREATED).body(ResponseHandler.response(replyId));
    }

    /**
     * 댓글 수정
     */
    @Operation(
            summary = "댓글 수정",
            description = "작성한 댓글을 수정합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "댓글 수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없음"),
            @ApiResponse(responseCode = "403", description = "수정 권한 없음")
    })
    @PutMapping("/groups/{groupId}/posts/{postId}/comments/{commentId}")
    public ResponseEntity<ResponseHandler<Long>> updateComment(
            @Parameter(description = "모임 ID")
            @PathVariable Long groupId,
            @Parameter(description = "게시글 ID")
            @PathVariable Long postId,
            @Parameter(description = "댓글 ID")
            @PathVariable Long commentId,
            @Parameter(description = "수정할 댓글 내용")
            @Valid @RequestPart CommentRequestDto request
    ) {
        Long userId = getCurrentUserId();
        User user = userQueryService.findById(userId);
        Long updatedCommentId = commentService.updateComment(commentId, user, request.getContent());
        return ResponseEntity.ok(ResponseHandler.response(updatedCommentId));
    }

    /**
     * 댓글 삭제
     */
    @Operation(
            summary = "댓글 삭제",
            description = "작성한 댓글을 삭제합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "댓글 삭제 성공"),
            @ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없음"),
            @ApiResponse(responseCode = "403", description = "삭제 권한 없음")
    })
    @DeleteMapping("/groups/{groupId}/posts/{postId}/comments/{commentId}")
    public ResponseEntity<ResponseHandler<Void>> deleteComment(
            @Parameter(description = "모임 ID")
            @PathVariable Long groupId,
            @Parameter(description = "게시글 ID")
            @PathVariable Long postId,
            @Parameter(description = "댓글 ID")
            @PathVariable Long commentId
    ) {
        Long userId = getCurrentUserId();
        User user = userQueryService.findById(userId);
        commentService.deleteComment(commentId, user);
        return ResponseEntity.ok(ResponseHandler.response(null));
    }

	/**
	 * 현재 사용자 ID 조회
	 */
	private Long getCurrentUserId() {
		CustomUserDetails principal =
			(CustomUserDetails) SecurityContextHolder.getContextHolderStrategy()
				.getContext()
				.getAuthentication()
				.getPrincipal();
		return principal.getUserId();
	}

	/**
	 * 현재 사용자 ID 조회
	 */
	private Long getCurrentUserIdOrNull() {
		try {
			return getCurrentUserId();
		} catch (Exception e) {
			return null;
		}
	}
}
