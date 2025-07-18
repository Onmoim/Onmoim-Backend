package com.onmoim.server.post.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onmoim.server.TestSecurityConfig;
import com.onmoim.server.post.dto.request.GroupPostRequestDto;
import com.onmoim.server.post.dto.request.CommentRequestDto;
import com.onmoim.server.post.dto.response.GroupPostResponseDto;
import com.onmoim.server.post.dto.response.CommentResponseDto;
import com.onmoim.server.post.dto.response.CommentThreadResponseDto;
import com.onmoim.server.post.dto.response.CursorPageResponseDto;
import com.onmoim.server.post.entity.GroupPostType;
import com.onmoim.server.post.service.GroupPostService;
import com.onmoim.server.post.service.PostLikeService;
import com.onmoim.server.post.service.CommentService;
import com.onmoim.server.post.service.CommentQueryService;
import com.onmoim.server.post.service.GroupPostQueryService;
import com.onmoim.server.post.entity.GroupPost;
import com.onmoim.server.user.entity.User;
import com.onmoim.server.user.service.UserQueryService;
import com.onmoim.server.security.CustomUserDetails;
import com.onmoim.server.security.JwtAuthenticationFilter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(
	controllers = GroupPostController.class,
	excludeFilters = {
		@ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
	}
)
@Import(TestSecurityConfig.class)
class GroupPostControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private GroupPostService groupPostService;

	@MockBean
	PostLikeService postLikeService;

	@MockBean
	CommentService commentService;

	@MockBean
	CommentQueryService commentQueryService;

	@MockBean
	GroupPostQueryService groupPostQueryService;

	@MockBean
	UserQueryService userQueryService;

	private GroupPostRequestDto requestDto;
	private GroupPostResponseDto responseDto;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders
			.webAppContextSetup(context)
			.apply(springSecurity())
			.build();

		requestDto = GroupPostRequestDto.builder()
			.title("Test Title")
			.content("Test Content")
			.type(GroupPostType.FREE)
			.build();

		responseDto = GroupPostResponseDto.builder()
			.id(1L)
			.groupId(1L)
			.authorId(1L)
			.authorName("testUser")
			.title("Test Title")
			.content("Test Content")
			.type(GroupPostType.FREE)
			.createdDate(LocalDateTime.now())
			.modifiedDate(LocalDateTime.now())
			.build();
	}

	private RequestPostProcessor authenticatedUser(Long userId) {
		CustomUserDetails userDetails = new CustomUserDetails(userId, "test@example.com", "google");
		return SecurityMockMvcRequestPostProcessors.user(userDetails);
	}

	@Test
	@DisplayName("게시글 작성 성공 테스트")
	void createPost() throws Exception {
		// given
		Long groupId = 1L;
		MockMultipartFile file1 = new MockMultipartFile("files", "test1.jpg", "image/jpeg", "test1".getBytes());
		MockMultipartFile file2 = new MockMultipartFile("files", "test2.jpg", "image/jpeg", "test2".getBytes());
		MockMultipartFile jsonPart = new MockMultipartFile("request", "", "application/json",
			objectMapper.writeValueAsString(requestDto).getBytes());

		given(groupPostService.createPost(eq(groupId), anyLong(), any(GroupPostRequestDto.class), any()))
			.willReturn(responseDto);

		// when & then
		mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/groups/{groupId}/posts", groupId)
				.file(file1)
				.file(file2)
				.file(jsonPart)
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.with(authenticatedUser(1L))
				.with(request -> {
					request.setMethod("POST");
					return request;
				}))
			.andDo(print())
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.id").value(1L))
			.andExpect(jsonPath("$.data.title").value("Test Title"))
			.andExpect(jsonPath("$.data.content").value("Test Content"));
	}

	@Test
	@DisplayName("게시글 상세 조회 성공 테스트")
	void getPostDetail() throws Exception {
		// given
		Long groupId = 1L;
		Long postId = 1L;

		given(groupPostService.getPostWithLikes(eq(groupId), eq(postId), anyLong()))
			.willReturn(responseDto);

		// when & then
		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/groups/{groupId}/posts/{postId}", groupId, postId)
				.with(authenticatedUser(1L)))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.id").value(1L))
			.andExpect(jsonPath("$.data.title").value("Test Title"))
			.andExpect(jsonPath("$.data.content").value("Test Content"));
	}

	@Test
	@DisplayName("게시글 수정 성공 테스트")
	void updatePost() throws Exception {
		// given
		Long groupId = 1L;
		Long postId = 1L;
		MockMultipartFile file1 = new MockMultipartFile("files", "test1.jpg", "image/jpeg", "test1".getBytes());
		MockMultipartFile file2 = new MockMultipartFile("files", "test2.jpg", "image/jpeg", "test2".getBytes());
		MockMultipartFile jsonPart = new MockMultipartFile("request", "", "application/json",
			objectMapper.writeValueAsString(requestDto).getBytes());

		given(groupPostService.updatePost(eq(groupId), eq(postId), anyLong(), any(GroupPostRequestDto.class), any()))
			.willReturn(responseDto);

		// when & then
		mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/groups/{groupId}/posts/{postId}", groupId, postId)
				.file(file1)
				.file(file2)
				.file(jsonPart)
				.with(authenticatedUser(1L))
				.with(request -> {
					request.setMethod("PUT");
					return request;
				})
				.contentType(MediaType.MULTIPART_FORM_DATA))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.id").value(1L))
			.andExpect(jsonPath("$.data.title").value("Test Title"))
			.andExpect(jsonPath("$.data.content").value("Test Content"));
	}

	@Test
	@DisplayName("게시글 삭제 성공 테스트")
	void deletePost() throws Exception {
		// given
		Long groupId = 1L;
		Long postId = 1L;

		doNothing().when(groupPostService).deletePost(eq(groupId), eq(postId), anyLong());

		// when & then
		mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/groups/{groupId}/posts/{postId}", groupId, postId)
				.with(authenticatedUser(1L)))
			.andDo(print())
			.andExpect(status().isOk());
	}

	@Test
	@DisplayName("게시글 좋아요 토글 성공 테스트 - 첫 좋아요")
	void togglePostLikeFirstLike() throws Exception {
		// given
		Long groupId = 1L;
		Long postId = 1L;

		given(groupPostService.togglePostLike(eq(groupId), eq(postId), anyLong()))
			.willReturn(true); // 좋아요 활성화

		// when & then
		mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/groups/{groupId}/posts/{postId}/like", groupId, postId)
				.with(authenticatedUser(1L)))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.isLiked").value(true));
	}

	@Test
	@DisplayName("게시글 좋아요 토글 성공 테스트 - 좋아요 취소")
	void togglePostLikeCancel() throws Exception {
		// given
		Long groupId = 1L;
		Long postId = 1L;

		given(groupPostService.togglePostLike(eq(groupId), eq(postId), anyLong()))
			.willReturn(false); // 좋아요 비활성화

		// when & then
		mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/groups/{groupId}/posts/{postId}/like", groupId, postId)
				.with(authenticatedUser(1L)))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.isLiked").value(false));
	}

	@Test
	@DisplayName("게시글 좋아요 토글 실패 테스트 - 인증되지 않은 사용자")
	void togglePostLikeUnauthorized() throws Exception {
		// given
		Long groupId = 1L;
		Long postId = 1L;

		// when & then
		mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/groups/{groupId}/posts/{postId}/like", groupId, postId))
			.andDo(print())
			.andExpect(status().isForbidden());
	}

	// ==================== 댓글 관련 테스트 ====================

	@Test
	@DisplayName("게시글 댓글 목록 조회 성공 테스트")
	void getComments() throws Exception {
		// given
		Long groupId = 1L;
		Long postId = 1L;

		CommentResponseDto comment1 = CommentResponseDto.builder()
				.id(1L)
				.content("첫 번째 댓글")
				.authorName("testUser")
				.replyCount(2L)
				.createdAt(LocalDateTime.now())
				.build();

		CommentResponseDto comment2 = CommentResponseDto.builder()
				.id(2L)
				.content("두 번째 댓글")
				.authorName("testUser")
				.replyCount(0L)
				.createdAt(LocalDateTime.now())
				.build();

		CursorPageResponseDto<CommentResponseDto> response = CursorPageResponseDto.<CommentResponseDto>builder()
				.content(java.util.Arrays.asList(comment1, comment2))
				.hasNext(false)
				.nextCursorId(null)
				.build();

		// Mock 게시글 엔티티 생성
		GroupPost mockPost = GroupPost.builder()
				.title("Test Title")
				.content("Test Content")
				.type(GroupPostType.FREE)
				.build();

		given(groupPostQueryService.validatePostReadAccess(eq(postId), eq(groupId)))
				.willReturn(mockPost);
		given(commentQueryService.getParentComments(eq(mockPost), any()))
				.willReturn(response);

		// when & then
		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/groups/{groupId}/posts/{postId}/comments", groupId, postId)
				.with(authenticatedUser(1L)))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content").isArray())
			.andExpect(jsonPath("$.data.content[0].id").value(1L))
			.andExpect(jsonPath("$.data.content[0].content").value("첫 번째 댓글"))
			.andExpect(jsonPath("$.data.content[0].replyCount").value(2L))
			.andExpect(jsonPath("$.data.content[1].id").value(2L))
			.andExpect(jsonPath("$.data.content[1].replyCount").value(0L));
	}

	@Test
	@DisplayName("댓글 스레드 조회 성공 테스트")
	void getCommentThread() throws Exception {
		// given
		Long groupId = 1L;
		Long postId = 1L;
		Long commentId = 1L;

		CommentResponseDto parentComment = CommentResponseDto.builder()
				.id(1L)
				.content("부모 댓글")
				.authorName("testUser")
				.replyCount(2L)
				.createdAt(LocalDateTime.now())
				.build();

		CommentResponseDto reply1 = CommentResponseDto.builder()
				.id(3L)
				.content("첫 번째 답글")
				.authorName("testUser2")
				.replyCount(0L)
				.createdAt(LocalDateTime.now())
				.build();

		CommentResponseDto reply2 = CommentResponseDto.builder()
				.id(4L)
				.content("두 번째 답글")
				.authorName("testUser3")
				.replyCount(0L)
				.createdAt(LocalDateTime.now())
				.build();

		CommentThreadResponseDto response = CommentThreadResponseDto.of(
				parentComment,
				java.util.Arrays.asList(reply1, reply2),
				null,
				false
		);

		given(commentQueryService.getCommentThread(eq(commentId), any()))
				.willReturn(response);

		// when & then
		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/groups/{groupId}/posts/{postId}/comments/{commentId}/thread",
				groupId, postId, commentId)
				.with(authenticatedUser(1L)))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.parentComment.id").value(1L))
			.andExpect(jsonPath("$.data.parentComment.content").value("부모 댓글"))
			.andExpect(jsonPath("$.data.parentComment.replyCount").value(2L))
			.andExpect(jsonPath("$.data.replies").isArray())
			.andExpect(jsonPath("$.data.replies[0].id").value(3L))
			.andExpect(jsonPath("$.data.replies[0].content").value("첫 번째 답글"))
			.andExpect(jsonPath("$.data.replies[1].id").value(4L))
			.andExpect(jsonPath("$.data.replies[1].content").value("두 번째 답글"));
	}

	@Test
	@DisplayName("댓글 작성 성공 테스트")
	void createComment() throws Exception {
		// given
		Long groupId = 1L;
		Long postId = 1L;
		CommentRequestDto requestDto = new CommentRequestDto("새로운 댓글입니다");

		// Mock 게시글 엔티티와 사용자 엔티티 생성
		GroupPost mockPost = GroupPost.builder()
				.title("Test Title")
				.content("Test Content")
				.type(GroupPostType.FREE)
				.build();
		User mockUser = User.builder()
				.name("testUser")
				.build();

		given(groupPostQueryService.validatePostAccess(eq(postId), eq(groupId), anyLong()))
				.willReturn(mockPost);
		given(userQueryService.findById(anyLong()))
				.willReturn(mockUser);
		given(commentService.createParentComment(eq(mockPost), eq(mockUser), eq("새로운 댓글입니다")))
				.willReturn(5L);

		// when & then
		mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/groups/{groupId}/posts/{postId}/comments", groupId, postId)
				.file(new MockMultipartFile("request", "", "application/json",
						objectMapper.writeValueAsString(requestDto).getBytes()))
				.with(authenticatedUser(1L))
				.with(request -> {
					request.setMethod("POST");
					return request;
				}))
			.andDo(print())
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data").value(5L));
	}

	@Test
	@DisplayName("답글 작성 성공 테스트")
	void createReply() throws Exception {
		// given
		Long groupId = 1L;
		Long postId = 1L;
		Long commentId = 1L;
		CommentRequestDto requestDto = new CommentRequestDto("답글입니다");

		// Mock 게시글 엔티티와 사용자 엔티티 생성
		GroupPost mockPost = GroupPost.builder()
				.title("Test Title")
				.content("Test Content")
				.type(GroupPostType.FREE)
				.build();
		User mockUser = User.builder()
				.name("testUser")
				.build();

		given(groupPostQueryService.validatePostAccess(eq(postId), eq(groupId), anyLong()))
				.willReturn(mockPost);
		given(userQueryService.findById(anyLong()))
				.willReturn(mockUser);
		given(commentService.createReply(eq(mockPost), eq(mockUser), eq(commentId), eq("답글입니다")))
				.willReturn(6L);

		// when & then
		mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/groups/{groupId}/posts/{postId}/comments/{commentId}/replies",
				groupId, postId, commentId)
				.file(new MockMultipartFile("request", "", "application/json",
						objectMapper.writeValueAsString(requestDto).getBytes()))
				.with(authenticatedUser(1L))
				.with(request -> {
					request.setMethod("POST");
					return request;
				}))
			.andDo(print())
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data").value(6L));
	}

	@Test
	@DisplayName("댓글 수정 성공 테스트")
	void updateComment() throws Exception {
		// given
		Long groupId = 1L;
		Long postId = 1L;
		Long commentId = 1L;
		CommentRequestDto requestDto = new CommentRequestDto("수정된 댓글입니다");

		// Mock 사용자 엔티티 생성
		User mockUser = User.builder()
				.name("testUser")
				.build();

		given(userQueryService.findById(anyLong()))
				.willReturn(mockUser);
		given(commentService.updateComment(eq(commentId), eq(mockUser), eq("수정된 댓글입니다")))
				.willReturn(1L);

		// when & then
		mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/groups/{groupId}/posts/{postId}/comments/{commentId}",
				groupId, postId, commentId)
				.file(new MockMultipartFile("request", "", "application/json",
						objectMapper.writeValueAsString(requestDto).getBytes()))
				.with(authenticatedUser(1L))
				.with(request -> {
					request.setMethod("PUT");
					return request;
				}))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data").value(1L));
	}

	@Test
	@DisplayName("댓글 삭제 성공 테스트")
	void deleteComment() throws Exception {
		// given
		Long groupId = 1L;
		Long postId = 1L;
		Long commentId = 1L;

		// Mock 사용자 엔티티 생성
		User mockUser = User.builder()
				.name("testUser")
				.build();

		given(userQueryService.findById(anyLong()))
				.willReturn(mockUser);
		given(commentService.deleteComment(eq(commentId), eq(mockUser)))
				.willReturn(commentId);

		// when & then
		mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/groups/{groupId}/posts/{postId}/comments/{commentId}",
				groupId, postId, commentId)
				.with(authenticatedUser(1L)))
			.andDo(print())
			.andExpect(status().isOk());
	}

	@Test
	@DisplayName("댓글 작성 실패 테스트 - 인증되지 않은 사용자")
	void createCommentUnauthorized() throws Exception {
		// given
		Long groupId = 1L;
		Long postId = 1L;
		CommentRequestDto requestDto = new CommentRequestDto("새로운 댓글입니다");

		// when & then
		mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/groups/{groupId}/posts/{postId}/comments", groupId, postId)
				.file(new MockMultipartFile("request", "", "application/json",
						objectMapper.writeValueAsString(requestDto).getBytes()))
				.with(request -> {
					request.setMethod("POST");
					return request;
				}))
			.andDo(print())
			.andExpect(status().isForbidden());
	}
}
