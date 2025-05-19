package com.onmoim.server.post.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onmoim.server.common.exception.GlobalExceptionHandler;
import com.onmoim.server.config.SecurityConfig;
import com.onmoim.server.post.dto.request.GroupPostRequestDto;
import com.onmoim.server.post.dto.response.GroupPostResponseDto;
import com.onmoim.server.post.entity.GroupPostType;
import com.onmoim.server.post.service.GroupPostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
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

@WebMvcTest(GroupPostController.class)
@ContextConfiguration(classes = {GroupPostController.class, GlobalExceptionHandler.class, SecurityConfig.class})
class GroupPostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GroupPostService groupPostService;

    private GroupPostRequestDto requestDto;
    private GroupPostResponseDto responseDto;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // DTO 생성
        requestDto = GroupPostRequestDto.builder()
                .title("Test Title")
                .content("Test Content")
                .type(GroupPostType.FREE)
                .build();

        // ResponseDTO 생성
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

    @Test
    @DisplayName("게시글 작성 성공 테스트")
    @WithMockUser(roles = "USER")
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
                .param("userId", "1")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .with(request -> {
                    request.setMethod("POST");
                    return request;
                }))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Test Title"))
                .andExpect(jsonPath("$.content").value("Test Content"));
    }

    @Test
    @DisplayName("게시글 상세 조회 성공 테스트")
    @WithMockUser(roles = "USER")
    void getPostDetail() throws Exception {
        // given
        Long groupId = 1L;
        Long postId = 1L;

        given(groupPostService.getPost(eq(groupId), eq(postId)))
                .willReturn(responseDto);

        // when & then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/groups/{groupId}/posts/{postId}", groupId, postId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Test Title"))
                .andExpect(jsonPath("$.content").value("Test Content"));
    }

    @Test
    @DisplayName("게시글 수정 성공 테스트")
    @WithMockUser(roles = "USER")
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
                .param("userId", "1")
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                })
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Test Title"))
                .andExpect(jsonPath("$.content").value("Test Content"));
    }

    @Test
    @DisplayName("게시글 삭제 성공 테스트")
    @WithMockUser(roles = "USER")
    void deletePost() throws Exception {
        // given
        Long groupId = 1L;
        Long postId = 1L;

        doNothing().when(groupPostService).deletePost(eq(groupId), eq(postId), anyLong());

        // when & then
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/groups/{groupId}/posts/{postId}", groupId, postId)
                .param("userId", "1"))
                .andDo(print())
                .andExpect(status().isNoContent());
    }
}
