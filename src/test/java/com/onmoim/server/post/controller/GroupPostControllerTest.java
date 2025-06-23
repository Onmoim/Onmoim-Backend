package com.onmoim.server.post.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onmoim.server.TestSecurityConfig;
import com.onmoim.server.post.dto.request.GroupPostRequestDto;
import com.onmoim.server.post.dto.response.GroupPostResponseDto;
import com.onmoim.server.post.entity.GroupPostType;
import com.onmoim.server.post.service.GroupPostService;
import com.onmoim.server.post.service.PostLikeService;
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
			.images(new ArrayList<>())
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
}
