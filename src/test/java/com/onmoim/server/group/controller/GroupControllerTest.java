package com.onmoim.server.group.controller;

import static com.onmoim.server.common.exception.ErrorCode.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import com.onmoim.server.chat.domain.dto.ChatRoomResponse;
import com.onmoim.server.chat.domain.enums.SubscribeRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onmoim.server.TestSecurityConfig;
import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.response.Message;
import com.onmoim.server.group.dto.request.GroupCreateRequestDto;
import com.onmoim.server.group.dto.request.GroupUpdateRequestDto;
import com.onmoim.server.group.service.GroupService;
import com.onmoim.server.meeting.service.MeetingService;
import com.onmoim.server.security.JwtAuthenticationFilter;


@WebMvcTest(
	controllers = GroupController.class,
	excludeFilters = {
		@ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
	}
)
@Import(TestSecurityConfig.class)
class GroupControllerTest {
	@Autowired
	private MockMvc mvc;

	@MockBean
	private GroupService groupService;

	@MockBean
	private MeetingService meetingService;

	private final ObjectMapper mapper = new ObjectMapper();

	@Test
	@WithMockUser(roles = "USER")
	@DisplayName("모임 생성 성공")
	void createGroupSuccess() throws Exception {
		// given
		given(groupService.createGroup(
			anyLong(),
			anyLong(),
			anyString(),
			anyString(),
			anyInt())).willReturn(
			new ChatRoomResponse(
				1L,
				"name",
				"description",
				1L,
				1L,
				SubscribeRegistry.CHAT_ROOM_SUBSCRIBE_PREFIX.getDestination())
		);

		var request = GroupCreateRequestDto.builder()
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
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.message").value(Message.SUCCESS.getDescription()))
			.andExpect(jsonPath("$.data.groupId").value(1L))
			.andDo(print());

		// then
		verify(groupService, times(1))
			.createGroup(
				anyLong(),
				anyLong(),
				anyString(),
				anyString(),
				anyInt());
	}

	@Test
	@DisplayName("모임 생성 실패 권한 부족")
	void createGroupFail1() throws Exception {
		// given
		var request = GroupCreateRequestDto.builder()
			.name("name")
			.description("description")
			.capacity(10)
			.categoryId(1L)
			.locationId(1L)
			.build();

		var json = mapper.writeValueAsString(request);

		// expected
		mvc.perform(post("/api/v1/groups")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isForbidden())
			.andDo(print());
	}

	@Test
	@DisplayName("모임 생성 카테고리 X")
	@WithMockUser(roles = "USER")
	void createGroupFail2() throws Exception {
		// given
		given(groupService.createGroup(
			anyLong(),
			anyLong(),
			anyString(),
			anyString(),
			anyInt())).willReturn(
			new ChatRoomResponse(
				1L,
				"name",
				"description",
				1L,
				1L,
				SubscribeRegistry.CHAT_ROOM_SUBSCRIBE_PREFIX.getDestination())
		);

		var request = GroupCreateRequestDto.builder()
			.name("name")
			.description("description")
			.capacity(10)
			.locationId(1L)
			.build();
		var json = mapper.writeValueAsString(request);

		// expected
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
	@DisplayName("모임 생성 카테고리 X, 정원 미달: 최소 정원 요건을 충족 X")
	@WithMockUser(roles = "USER")
	void createGroupFail3() throws Exception {
		// given
		given(groupService.createGroup(
			anyLong(),
			anyLong(),
			anyString(),
			anyString(),
			anyInt())).willReturn(
			new ChatRoomResponse(
				1L,
				"name",
				"description",
				1L,
				1L,
				SubscribeRegistry.CHAT_ROOM_SUBSCRIBE_PREFIX.getDestination())
		);

		var request = GroupCreateRequestDto.builder()
			.name("name")
			.description("description")
			.capacity(4)
			.locationId(1L)
			.build();

		var json = mapper.writeValueAsString(request);

		// expected
		mvc.perform(post("/api/v1/groups")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value(Message.BAD_REQUEST.getDescription()))
			.andExpect(jsonPath("$.data.length()").value(2))
			.andDo(print());
	}

	@Test
	@DisplayName("모임 수정 성공")
	@WithMockUser(roles = "USER")
	void updateGroupSuccess() throws Exception {
		// given
		doNothing().when(groupService).updateGroup(
			anyLong(),
			anyString(),
			anyInt(),
			any());

		// path variable
		var groupId = 1L;
		// img
		var file = new MockMultipartFile(
			"file",
			"test.jpeg",
			MediaType.IMAGE_JPEG_VALUE,
			"<<test data>>".getBytes(StandardCharsets.UTF_8)
		);
		// json
		var jsonPart = new MockMultipartFile(
			"request",
			null,
			MediaType.APPLICATION_JSON_VALUE,
			mapper.writeValueAsBytes(GroupUpdateRequestDto.builder()
				.description("description")
				.capacity(10)
				.build()
			)
		);
		// expected
		mvc.perform(multipart(
				HttpMethod.PATCH,
				"/api/v1/groups/{groupId}", groupId)
				.file(file)
				.file(jsonPart)
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value(Message.SUCCESS.getDescription()))
			.andExpect(jsonPath("$.data").value("수정 성공"));
	}

	@Test
	@DisplayName("모임 가입 성공")
	@WithMockUser(roles = "USER")
	void joinGroupSuccess() throws Exception {
		// given
		given(groupService.joinGroup(anyLong())).willReturn("topic");

		// expected
		mvc.perform(post("/api/v1/groups/{groupId}/join", 1L))
			.andExpect(status().is2xxSuccessful())
			.andExpect(jsonPath("$.message").value(Message.SUCCESS.getDescription()))
			.andExpect(jsonPath("$.data.subscribeDestination").value("topic"))
			.andDo(print());
	}

	@Test
	@DisplayName("모임 가입 실패: 권한 부족")
	void joinGroupFailure1() throws Exception {
		// expected
		mvc.perform(post("/api/v1/groups/{groupId}/join", 1L))
			.andExpect(status().isForbidden())
			.andDo(print());
	}

	@Test
	@DisplayName("모임 가입 실패: 잘못된 요청(이미 가입(Member, Owner))")
	@WithMockUser(roles = "USER")
	void joinGroupFailure2() throws Exception {
		// given
		doThrow(new CustomException(GROUP_ALREADY_JOINED))
			.when(groupService).joinGroup(anyLong());

		// expected
		mvc.perform(post("/api/v1/groups/{groupId}/join", 1L))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value(GROUP_ALREADY_JOINED.toString()))
			.andExpect(jsonPath("$.data").value(GROUP_ALREADY_JOINED.getDetail()))
			.andDo(print());
	}
}
