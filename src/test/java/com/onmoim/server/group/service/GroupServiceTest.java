package com.onmoim.server.group.service;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;
import com.onmoim.server.group.entity.Group;
import com.onmoim.server.group.entity.GroupUser;
import com.onmoim.server.group.entity.Status;
import com.onmoim.server.group.repository.GroupRepository;
import com.onmoim.server.group.repository.GroupUserRepository;
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

	@AfterEach
	void tearDown() {
		groupUserRepository.deleteAll();
		groupRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	@DisplayName("동시에 가입 요청 테스트")
	void joinGroupConcurrencyTest() throws InterruptedException {
		// given
		Group group = groupRepository.save(Group.groupCreateBuilder()
			.name("group")
			.capacity(10)
			.build());
		User owner = User.builder().name("모임장").build();
		userRepository.save(owner);
		groupUserRepository.save(GroupUser.create(group, owner, Status.OWNER));

		List<User> userList = new ArrayList<>();
		for (int i = 0; i < 20; i++) {
			User user = userRepository.save(User.builder()
				.name("test" + i)
				.build());
			userList.add(user);
		}
		// when
		int taskCount = 20;
		ExecutorService executorService = Executors.newFixedThreadPool(10);
		CountDownLatch latch = new CountDownLatch(taskCount);
		for (int i = 0; i < taskCount; i++) {
			final int idx = i;
			executorService.submit(() -> {
				try {
					// 각 쓰레드마다 인증 정보 세팅
					Long userId = userList.get(idx).getId();
					var detail = new CustomUserDetails(userId, "test", "test");
					var authenticated = UsernamePasswordAuthenticationToken.authenticated(
						detail, null, null);
					SecurityContextHolder.getContext().setAuthentication(authenticated);

					// 동시성 테스트
					groupService.joinGroup(group.getId());
				} finally {
					// 쓰레드 인증 정보 삭제
					SecurityContextHolder.clearContext();
					latch.countDown();
				}
			});
		}
		latch.await();
		executorService.shutdown();

		// then
		Group updatedGroup = groupRepository.findById(group.getId()).orElseThrow();
		Long count = groupUserRepository.countByGroupAndStatus(group.getId(), List.of(Status.MEMBER, Status.OWNER));
		System.out.println("최종 모임 인원 by GroupUser = " + count);
		assertThat(count).isLessThanOrEqualTo(updatedGroup.getCapacity());
		assertThat(count).isEqualTo(updatedGroup.getCapacity());
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

		Group group = Group.groupCreateBuilder()
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
		Long likeCount = groupUserRepository.countByGroupAndStatus(group.getId(), List.of(Status.BOOKMARK));
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

		Group group = Group.groupCreateBuilder()
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
		Long likeCount = groupUserRepository.countByGroupAndStatus(group.getId(), List.of(Status.BOOKMARK));
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

		Group group = Group.groupCreateBuilder()
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

		Group group = Group.groupCreateBuilder()
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
		Long likeCount = groupUserRepository.countByGroupAndStatus(group.getId(), List.of(Status.PENDING));
		assertThat(likeCount).isEqualTo(1);

		SecurityContextHolder.clearContext();
	}
}
