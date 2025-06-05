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

	private void setSecurityContext(Long userId) {
		var detail = new CustomUserDetails(
			userId,
			null,
			null
		);
		var authenticated = UsernamePasswordAuthenticationToken.authenticated(
				detail,
				null,
				null
		);
		SecurityContextHolder.getContext().setAuthentication(authenticated);
	}

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

		setSecurityContext(user.getId());

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

		setSecurityContext(user.getId());

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

		setSecurityContext(user.getId());

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

		setSecurityContext(user.getId());

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

		// 모임원: 34명  모임장: 1
		for (int i = 0; i < 35; i++) {
			User user = User.builder().build();
			userRepository.save(user);
			if (i == 0) {
				groupUserRepository.save(GroupUser.create(group, user, Status.OWNER));
				continue;
			}
			groupUserRepository.save(GroupUser.create(group, user, Status.MEMBER));
		}

		// expected
		var size = 10;
		var groupId = group.getId();

		Long totalCount = groupService.groupMemberCount(groupId);
		assertThat(totalCount).isEqualTo(35);

		// 1 ~ 11
		List<GroupUser> groupMembers1 = groupService.getGroupMembers(
			groupId,
			null,
			size);

		assertThat(groupMembers1.size()).isEqualTo(size + 1);
		groupMembers1.removeLast();

		Long cursorId1 = groupMembers1.getLast().getId().getUserId();
		System.out.println("cursorId1 = " + cursorId1);
		System.out.println(groupMembers1);

		// 11 ~ 20
		List<GroupUser> groupMembers2 = groupService.getGroupMembers(
			groupId,
			cursorId1,
			size);

		assertThat(groupMembers2.size()).isEqualTo(size + 1);
		groupMembers2.removeLast();

		Long cursorId2 = groupMembers2.getLast().getId().getUserId();
		System.out.println("cursorId2 = " + cursorId2);
		System.out.println(groupMembers2);

		// 21 ~ 30
		List<GroupUser> groupMembers3 = groupService.getGroupMembers(
			groupId,
			cursorId2,
			size);

		assertThat(groupMembers3.size()).isEqualTo(size + 1);
		groupMembers3.removeLast();

		Long cursorId3 = groupMembers3.getLast().getId().getUserId();
		System.out.println("cursorId3 = " + cursorId3);
		System.out.println(groupMembers3);

		// 31 ~ 35
		List<GroupUser> groupMembers4 = groupService.getGroupMembers(
			groupId,
			cursorId3,
			size
		);

		System.out.println(groupMembers4);
		assertThat(groupMembers4.size()).isEqualTo(5);
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

		setSecurityContext(user.getId());

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
		GroupUser groupUser = groupUserRepository.findGroupUser(groupId, user.getId()).get();
		assertThat(groupUser).isNotNull();
		assertThat(groupUser.getStatus()).isEqualTo(Status.OWNER);
	}
}
