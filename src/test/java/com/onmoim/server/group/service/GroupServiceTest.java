package com.onmoim.server.group.service;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.onmoim.server.group.entity.Group;
import com.onmoim.server.group.repository.GroupRepository;
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
	@Test
	@DisplayName("동시에 가입 요청 테스트")
	void joinGroup_concurrencyTest() throws InterruptedException {
		// given
		Group group = groupRepository.save(Group.groupCreateBuilder()
			.name("group")
			.capacity(10)
			.participantCount(1)
			.build());

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
		assertThat(updatedGroup.getParticipantCount()).isLessThanOrEqualTo(updatedGroup.getCapacity());
		System.out.println("최종 모임 인원 = " + updatedGroup.getParticipantCount());
	}
}
