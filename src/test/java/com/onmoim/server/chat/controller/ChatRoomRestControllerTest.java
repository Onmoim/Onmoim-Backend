package com.onmoim.server.chat.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onmoim.server.chat.dto.ChatRoomResponse;
import com.onmoim.server.chat.dto.CreateRoomRequest;
import com.onmoim.server.chat.service.ChatRoomService;
import com.onmoim.server.common.exception.GlobalExceptionHandler;
import com.onmoim.server.config.SecurityConfig;

@WebMvcTest(ChatRoomRestController.class)
@ContextConfiguration(classes = {ChatRoomRestController.class, SecurityConfig.class, GlobalExceptionHandler.class})
public class ChatRoomRestControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private ChatRoomService chatRoomService;

	private CreateRoomRequest createRoomRequest;
	private ChatRoomResponse chatRoomResponse;

	@BeforeEach
	void setUp() {
		// 테스트 데이터 준비
		createRoomRequest = new CreateRoomRequest();
		createRoomRequest.setName("Test Chat Room");
		createRoomRequest.setDescription("Test Description");

		chatRoomResponse = new ChatRoomResponse();
		chatRoomResponse.setId(1L);
		chatRoomResponse.setName("Test Chat Room");
		chatRoomResponse.setDescription("Test Description");
		chatRoomResponse.setCreatorId("user-id");
	}

	@Test
	@WithMockUser()
	@DisplayName("채팅방 생성 API 테스트")
	void testCreateRoom() throws Exception {
		// given
		when(chatRoomService.createRoom(any(), any(), any()))
			.thenReturn(chatRoomResponse);

		// when & then
		mockMvc.perform(post("/api/v1/chat/room")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(createRoomRequest)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.id").value(1L))
			.andExpect(jsonPath("$.data.name").value("Test Chat Room"))
			.andExpect(jsonPath("$.data.description").value("Test Description"))
			.andExpect(jsonPath("$.data.creatorId").value("user-id"));
	}
	
	@Test
	@WithMockUser()
	@DisplayName("채팅방 이름이 비어있을 때 유효성 검증 실패 테스트")
	void testCreateRoomWithEmptyName() throws Exception {
		// given
		CreateRoomRequest invalidRequest = new CreateRoomRequest();
		invalidRequest.setName(""); // 빈 이름
		invalidRequest.setDescription("Test Description");

		// when & then
		mockMvc.perform(post("/api/v1/chat/room")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(invalidRequest)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.data[0].message").value("채팅방 이름은 필수입니다."));
	}
	
	@Test
	@WithMockUser()
	@DisplayName("채팅방 이름이 null일 때 유효성 검증 실패 테스트")
	void testCreateRoomWithNullName() throws Exception {
		// given
		CreateRoomRequest invalidRequest = new CreateRoomRequest();
		invalidRequest.setName(null); // null 이름
		invalidRequest.setDescription("Test Description");

		// when & then
		mockMvc.perform(post("/api/v1/chat/room")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(invalidRequest)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.data[0].message").value("채팅방 이름은 필수입니다."));
	}
	
	@Test
	@WithMockUser()
	@DisplayName("채팅방 이름이 공백만 있을 때 유효성 검증 실패 테스트")
	void testCreateRoomWithWhitespaceName() throws Exception {
		// given
		CreateRoomRequest invalidRequest = new CreateRoomRequest();
		invalidRequest.setName("   "); // 공백만 있는 이름
		invalidRequest.setDescription("Test Description");

		// when & then
		mockMvc.perform(post("/api/v1/chat/room")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(invalidRequest)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.data[0].message").value("채팅방 이름은 필수입니다."));
	}
	
	@Test
	@WithMockUser()
	@DisplayName("설명이 null이어도 유효성 검증 성공 테스트")
	void testCreateRoomWithNullDescription() throws Exception {
		// given
		CreateRoomRequest validRequest = new CreateRoomRequest();
		validRequest.setName("Test Chat Room");
		validRequest.setDescription(null); // null 설명
		
		when(chatRoomService.createRoom(any(), any(), any()))
			.thenReturn(chatRoomResponse);

		// when & then
		mockMvc.perform(post("/api/v1/chat/room")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(validRequest)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.message").value("SUCCESS"));
	}
}