package com.onmoim.server.group.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;
import com.onmoim.server.common.exception.GlobalExceptionHandler;
import com.onmoim.server.common.response.Message;
import com.onmoim.server.config.SecurityConfig;
import com.onmoim.server.group.dto.request.CreateGroupRequestDto;
import com.onmoim.server.group.service.GroupService;

@WebMvcTest(GroupController.class)
@ContextConfiguration(classes = {GroupController.class, GlobalExceptionHandler.class, SecurityConfig.class})
class GroupControllerTest {
	@Autowired
	private MockMvc mvc;

	@MockBean
	private GroupService groupService;

	private final ObjectMapper mapper = new ObjectMapper();

	@Test
	@WithMockUser(roles = "USER")
	void createGroupSuccess() throws Exception {
		// given
		given(groupService.createGroup(any())).willReturn(1L);
		var request = CreateGroupRequestDto.builder()
			.name("name")
			.description("description")
			.capacity(10)
			.categoryId(1L)
			.locationId(1L)
			.build();

		var json = mapper.writeValueAsString(request);

		// when
		mvc.perform(post("/api/v1/groups")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value(Message.SUCCESS.getDescription()))
			.andExpect(jsonPath("$.data").value(1L))
			.andDo(print());

		// then
		verify(groupService, times(1)).createGroup(any());
	}

	@Test
	@DisplayName("그룹 생성 실패 권한 부족")
	void createGroupFail1() throws Exception {
		// given
		given(groupService.createGroup(any())).willReturn(1L);
		var request = CreateGroupRequestDto.builder()
			.name("name")
			.description("description")
			.capacity(10)
			.categoryId(1L)
			.locationId(1L)
			.build();
		var json = mapper.writeValueAsString(request);

		// when
		mvc.perform(post("/api/v1/groups")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isForbidden())
			.andDo(print());
	}

	@Test
	@DisplayName("그룹 생성 카테고리 X")
	@WithMockUser(roles = "USER")
	void createGroupFail2() throws Exception {
		// given
		given(groupService.createGroup(any())).willReturn(1L);
		var request = CreateGroupRequestDto.builder()
			.name("name")
			.description("description")
			.capacity(10)
			.locationId(1L)
			.build();
		var json = mapper.writeValueAsString(request);

		// when
		mvc.perform(post("/api/v1/groups")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value(Message.BAD_REQUEST.getDescription()))
			.andExpect(jsonPath("$.data.length()").value(1))
			.andExpect(jsonPath("$.data[0].field")
				.value("categoryId"))
			.andExpect(jsonPath("$.data[0].message")
				.value("모임 카테고리 설정은 필수입니다."))
			.andDo(print());
	}

	@Test
	@DisplayName("그룹 생성 카테고리 X, 정원 미달: 최소 정원 요건을 충족 X")
	@WithMockUser(roles = "USER")
	void createGroupFail3() throws Exception {
		// given
		given(groupService.createGroup(any())).willReturn(1L);
		var request = CreateGroupRequestDto.builder()
			.name("name")
			.description("description")
			.capacity(4)
			.locationId(1L)
			.build();
		var json = mapper.writeValueAsString(request);

		// when
		mvc.perform(post("/api/v1/groups")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value(Message.BAD_REQUEST.getDescription()))
			.andExpect(jsonPath("$.data.length()").value(2))
			.andDo(print());
	}

	@Test
	@DisplayName("그룹 가입 성공")
	@WithMockUser(roles = "USER")
	void joinGroupSuccess() throws Exception {
		// given
		doNothing().when(groupService).joinGroup(anyLong());

		// when
		mvc.perform(post("/api/v1/groups/{groupId}/join", 1L))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value(Message.SUCCESS.getDescription()))
			.andExpect(jsonPath("$.data").value("모임 가입 성공"))
			.andDo(print());
	}

	@Test
	@DisplayName("그룹 가입 실패: 권한 부족")
	void joinGroupFailure1() throws Exception {
		// when
		mvc.perform(post("/api/v1/groups/{groupId}/join", 1L))
			.andExpect(status().isForbidden())
			.andDo(print());
	}

	@Test
	@DisplayName("그룹 가입 실패: 잘못된 요청(이미 가입(Member, Owner))")
	@WithMockUser(roles = "USER")
	void joinGroupFailure2() throws Exception {
		// given
		doThrow(new CustomException(ErrorCode.INVALID_GROUP_JOIN))
			.when(groupService).joinGroup(anyLong());

		// when
		mvc.perform(post("/api/v1/groups/{groupId}/join", 1L))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value(ErrorCode.INVALID_GROUP_JOIN.toString()))
			.andExpect(jsonPath("$.data").value(ErrorCode.INVALID_GROUP_JOIN.getDetail()))
			.andDo(print());
	}
}
