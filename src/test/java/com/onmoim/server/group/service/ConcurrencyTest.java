package com.onmoim.server.group.service;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import com.onmoim.server.group.entity.Group;
import com.onmoim.server.group.entity.GroupUser;
import com.onmoim.server.group.entity.Status;
import com.onmoim.server.group.repository.GroupRepository;
import com.onmoim.server.group.repository.GroupUserRepository;
import com.onmoim.server.security.CustomUserDetails;
import com.onmoim.server.user.entity.User;
import com.onmoim.server.user.repository.UserRepository;

@SpringBootTest
class ConcurrencyTest {
	@Autowired
	GroupService groupService;
	@Autowired
	GroupRepository groupRepository;
	@Autowired
	GroupUserRepository groupUserRepository;
	@Autowired
	UserRepository userRepository;

	@AfterEach
	void tearDown() {
		groupUserRepository.deleteAll();
		groupRepository.deleteAll();
		userRepository.deleteAll();
	}

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
	@DisplayName("동시에 가입 요청 테스트")
	@Transactional
	void joinGroupConcurrencyTest() throws InterruptedException {
		// given
		assertThat(TestTransaction.isActive()).isTrue();

		Group group = Group.builder()
			.name("group")
			.capacity(10)
			.build();

		groupRepository.save(group);
		final Long groupId = group.getId();

		List<User> userList = new ArrayList<>();

		User owner = User.builder().name("모임장").build();
		userList.add(owner);

		groupUserRepository.save(GroupUser.create(group, owner, Status.OWNER));
		userRepository.save(owner);

		IntStream.range(0, 20).forEach(i -> {
			User user = User.builder()
				.name("test" + i)
				.build();
			userList.add(user);
			userRepository.save(user);
		});

		TestTransaction.flagForCommit(); // 트랜잭션 커밋
		TestTransaction.end();           // 트랜잭션 종료

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
					setSecurityContext(userId);

					// 동시성 테스트
					groupService.joinGroup(groupId);
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
		Group updatedGroup = groupRepository.findById(groupId).orElseThrow();
		Long count = groupUserRepository.countByGroupAndStatuses(groupId, List.of(Status.MEMBER, Status.OWNER));
		System.out.println("최종 모임 인원 by GroupUser = " + count);
		assertThat(count).isLessThanOrEqualTo(updatedGroup.getCapacity());
		assertThat(count).isEqualTo(updatedGroup.getCapacity());
	}
}
