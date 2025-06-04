package com.onmoim.server.group.service;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import com.onmoim.server.category.entity.Category;
import com.onmoim.server.category.repository.CategoryRepository;
import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;
import com.onmoim.server.group.dto.request.GroupCreateRequestDto;
import com.onmoim.server.group.dto.response.CursorPageResponseDto;
import com.onmoim.server.group.dto.response.GroupMembersResponseDto;
import com.onmoim.server.group.entity.Group;
import com.onmoim.server.group.entity.GroupUser;
import com.onmoim.server.group.entity.Status;
import com.onmoim.server.group.repository.GroupRepository;
import com.onmoim.server.group.repository.GroupUserRepository;
import com.onmoim.server.location.entity.Location;
import com.onmoim.server.location.repository.LocationRepository;
import com.onmoim.server.security.CustomUserDetails;
import com.onmoim.server.user.entity.User;
import com.onmoim.server.user.repository.UserRepository;

@SpringBootTest
class GroupServiceTest {
	@Autowired
	private GroupService groupService;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private GroupRepository groupRepository;
	@Autowired
	private GroupUserRepository groupUserRepository;
	@Autowired
	private LocationRepository locationRepository;
	@Autowired
	private CategoryRepository categoryRepository;

	@Test
	@DisplayName("모임 좋아요: 성공")
	@Transactional
	void likeGroupSuccessTest() {
		// given
		User user = User.builder()
			.name("test")
			.build();
		userRepository.save(user);

		Group group = Group.builder()
			.name("group")
			.description("description")
			.capacity(100)
			.build();
		groupRepository.save(group);

		var detail = new CustomUserDetails(user.getId(), "test", "test");
		var authenticated = UsernamePasswordAuthenticationToken.authenticated(
			detail, null, null);
		SecurityContextHolder.getContext().setAuthentication(authenticated);

		// when
		groupService.likeGroup(group.getId());

		// then
		Long likeCount = groupUserRepository.countByGroupAndStatuses(group.getId(), List.of(Status.BOOKMARK));
		assertThat(likeCount).isEqualTo(1);

		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("모임 좋아요 성공: PENDING -> BOOKMARK ")
	@Transactional
	void likeGroupSuccessTest2() {
		// given
		User user = User.builder()
			.name("test")
			.build();
		userRepository.save(user);

		Group group = Group.builder()
			.name("group")
			.description("description")
			.capacity(100)
			.build();
		groupRepository.save(group);
		groupUserRepository.save(GroupUser.create(group, user, Status.PENDING));

		var detail = new CustomUserDetails(user.getId(), "test", "test");
		var authenticated = UsernamePasswordAuthenticationToken.authenticated(
			detail, null, null);
		SecurityContextHolder.getContext().setAuthentication(authenticated);

		// when
		groupService.likeGroup(group.getId());

		// then
		Long likeCount = groupUserRepository.countByGroupAndStatuses(group.getId(), List.of(Status.BOOKMARK));
		assertThat(likeCount).isEqualTo(1);

		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("모임 좋아요 실패: 이미 가입")
	@Transactional
	void likeGroupFailureTest() {
		// given
		User user = User.builder()
			.name("test")
			.build();
		userRepository.save(user);

		Group group = Group.builder()
			.name("group")
			.description("description")
			.capacity(100)
			.build();
		groupRepository.save(group);
		groupUserRepository.save(GroupUser.create(group, user, Status.MEMBER));

		var detail = new CustomUserDetails(user.getId(), "test", "test");
		var authenticated = UsernamePasswordAuthenticationToken.authenticated(
			detail, null, null);
		SecurityContextHolder.getContext().setAuthentication(authenticated);

		assertThatThrownBy(() -> groupService.likeGroup(group.getId()))
			.isInstanceOf(CustomException.class)
			.hasMessage(ErrorCode.GROUP_ALREADY_JOINED.getDetail());

		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("모임 좋아요 취소 성공: BOOKMARK -> PENDING")
	@Transactional
	void likeCancelGroupSuccessTest() {
		// given
		User user = User.builder()
			.name("test")
			.build();
		userRepository.save(user);

		Group group = Group.builder()
			.name("group")
			.description("description")
			.capacity(100)
			.build();
		groupRepository.save(group);
		groupUserRepository.save(GroupUser.create(group, user, Status.BOOKMARK));

		var detail = new CustomUserDetails(user.getId(), "test", "test");
		var authenticated = UsernamePasswordAuthenticationToken.authenticated(
			detail, null, null);
		SecurityContextHolder.getContext().setAuthentication(authenticated);

		// when
		groupService.likeGroup(group.getId());

		// then
		Long likeCount = groupUserRepository.countByGroupAndStatuses(group.getId(), List.of(Status.PENDING));
		assertThat(likeCount).isEqualTo(1);

		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("모임 회원 조회")
	@Transactional
	void selectGroupMembers() {
		// given
		Group group = Group.builder()
			.name("group")
			.description("description")
			.capacity(100)
			.build();
		groupRepository.save(group);

		// 유저: 40  모임장: 1 모임원: 19
		for (int i = 0; i < 40; i++) {
			User user = User.builder()
				.name("test" + i)
				.profileImgUrl("img_url" + i)
				.build();
			userRepository.save(user);
			if (i >= 20) continue;
			if (i == 0) {
				groupUserRepository.save(GroupUser.create(group, user, Status.OWNER));
				continue;
			}
			groupUserRepository.save(GroupUser.create(group, user, Status.MEMBER));
		}

		// expected

		// 1 ~ 10
		CursorPageResponseDto<GroupMembersResponseDto> result1 =
			groupService.getGroupMembers(group.getId(), null, 10);

		List<GroupMembersResponseDto> content1 = result1.getContent();
		assertThat(result1.getTotalCount()).isEqualTo(20);
		assertThat(result1.isHasNext()).isTrue();
		assertThat(content1.size()).isEqualTo(10);
		System.out.println(content1);
		Long cursorId1 = result1.getCursorId();

		// 11 ~ 20
		CursorPageResponseDto<GroupMembersResponseDto> result2 =
			groupService.getGroupMembers(group.getId(), cursorId1, 10);

		List<GroupMembersResponseDto> content2 = result2.getContent();
		assertThat(result2.isHasNext()).isFalse();
		assertThat(result2.getCursorId()).isNotNull();
		assertThat(content2.size()).isEqualTo(10);
		System.out.println(content2);

		CursorPageResponseDto<GroupMembersResponseDto> result3 =
			groupService.getGroupMembers(group.getId(), 50L, 10);

		List<GroupMembersResponseDto> content3 = result3.getContent();
		assertThat(result3.isHasNext()).isFalse();
		assertThat(result3.getCursorId()).isNull();
		assertThat(content3).isEmpty();
	}

	@Test
	@DisplayName("모임 생성 성공")
	@Transactional
	void createGroupSuccess() {
		// given
		Location location = Location.create("1234", "서울특별시", "종로구", "청운동", null);
		locationRepository.save(location);

		Category category = Category.create("음악", null);
		categoryRepository.save(category);

		User user = User.builder().name("test").build();
		userRepository.save(user);

		String name = "테스트 모임";
		String description = "모임 설명";
		int capacity = 100;

		var detail = new CustomUserDetails(user.getId(), "test", "test");
		var authenticated = UsernamePasswordAuthenticationToken.authenticated(
			detail, null, null);
		SecurityContextHolder.getContext().setAuthentication(authenticated);

		// when
		Long groupId = groupService.createGroup(
			category.getId(),
			location.getId(),
			name,
			description,
			capacity
		);

		// then
		Group group = groupRepository.getById(groupId);
		assertThat(group.getName()).isEqualTo(name);
		assertThat(group.getDescription()).isEqualTo(description);
		assertThat(group).isNotNull();
	}
}
